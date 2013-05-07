(ns geppetto.claim
  (:require [clojure.string :as str])
  (:use [clojure.pprint :only [pprint]])
  (:use [clojure.walk])
  (:use [geppetto.parameters :only [read-params]])
  (:use [geppetto.records :only [run-with-new-record]])
  (:use [geppetto.stats])
  (:use [geppetto.random]))

(def results (ref []))

(defn replace-keys
  [resultstype form]
  (if (and (keyword? form) (re-matches #"^_.*" (name form)))
    (let [k (keyword (str/replace (name form) #"^_" ""))]
      `(map (fn [r#] (get r# ~k))
          (map (fn [rs-alltypes#]
               (get rs-alltypes# ~resultstype))
             @results)))
    form))

(defmacro make-claim
  [claim-name & opts]
  (apply merge
         `{:name '~claim-name}
         (for [[opt & params] opts]
           (cond (= 'parameters opt)
                 (let [p (first params)]
                   `{:parameters ~p})
                 (= 'depends opt)
                 (let [d (first params)]
                   `{:depends ~d})
                 (= 'verify opt)
                 `{:verify
                   ~(zipmap
                     [:control :comparison :comparative]
                     (for [resultstype [:control :comparison :comparative]]
                       (vec
                        (for [v (get (first params) resultstype)]
                          (let [result-fn (postwalk #(replace-keys resultstype %) v)]
                            `{:code '~v
                              :result (fn [] ~result-fn)})))))}))))

(defn evaluate-claim
  [run-fn claim datadir git recordsdir nthreads]
  (let [seed 1
        repetitions 30
        run-results (binding [rgen (new-seed seed)]
                      (run-with-new-record
                        run-fn (:parameters claim) datadir seed git recordsdir
                        nthreads repetitions false true true))]
    (dosync (alter results (constantly run-results)))
    (let [verifications (apply concat
                               (for [resultstype [:control :comparison :comparative]]
                                 (for [to-verify (get (:verify claim) resultstype)]
                                   {:code (:code to-verify)
                                    :resultstype resultstype
                                    :verification-result ((:result to-verify))})))]
      (doseq [ver verifications]
        (println (format "%s (%s):" (if (:verification-result ver) "PASS" "FAIL")
                    (name (:resultstype ver))))
        (pprint (:code ver)))
      (if (every? :verification-result verifications)
        (do
          (println (format "Claim \"%s\" verified." (:name claim)))
          true)
        (do
          (println (format "Claim \"%s\" not verified." (:name claim)))
          false)))))
