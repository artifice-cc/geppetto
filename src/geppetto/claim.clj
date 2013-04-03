(ns geppetto.claim
  (:require [clojure.string :as str])
  (:use [clojure.walk])
  (:use [geppetto.parameters :only [read-params]])
  (:use [geppetto.records :only [run-with-new-record read-archived-results]])
  (:use [geppetto.stats])
  (:use [geppetto.random]))

(defn replace-keys
  [results resultstype form]
  (if (and (keyword? form) (re-matches #"^_.*" (name form)))
    (let [k (keyword (str/replace (name form) #"^_" ""))]
      `(map (fn [r#] (get r# ~k))
          (map (fn [rs-alltypes#]
               (last (get rs-alltypes# ~resultstype)))
             ;; put it in a vec so it is not evaled as ({:control ...})
             ;; but rather as [{:control ...}]
             [~@results])))
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
                       (vec (for [v (get (first params) resultstype)]
                              `{:code '~v
                                :result (fn [results#]
                                          (eval (postwalk
                                                 (fn [term#]
                                                   (replace-keys results# ~resultstype term#))
                                                 '~v)))}))))}))))

(defn evaluate-claim
  [run-fn claim db-params datadir git recordsdir nthreads]
  (println)
  (let [seed 1
        repetitions 30
        results (binding [rgen (new-seed seed)]
                  (run-with-new-record
                    run-fn db-params datadir seed git recordsdir
                    nthreads repetitions false true true))
        verifications (apply concat
                             (for [resultstype [:control :comparison :comparative]]
                               (for [to-verify (get (:verify claim) resultstype)]
                                 {:code (:code to-verify)
                                  :resultstype resultstype
                                  :verification-result ((:result to-verify) results)})))]
    (println)
    (doseq [ver verifications]
      (println (format "%s (%s):\t %s" (if (:verification-result ver) "PASS" "FAIL")
                  (name (:resultstype ver)) (:code ver))))
    (if (every? :verification-result verifications)
      (do
        (println (format "Claim \"%s\" verified." (:name claim)))
        true)
      (do
        (println (format "Claim \"%s\" not verified." (:name claim)))
        false))))
