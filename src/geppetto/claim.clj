(ns geppetto.claim
  (:use [clojure.test])
  (:require [clojure.string :as str])
  (:use [clojure.pprint :only [pprint]])
  (:use [clojure.walk])
  (:use [geppetto.parameters :only [read-params]])
  (:use [geppetto.records :only [run-with-new-record]])
  (:use [geppetto.stats])
  (:use [geppetto.random])
  (:require [taoensso.timbre :as timbre]))

(def results (ref []))

(defn replace-keys
  [resultstype form]
  (if (and (keyword? form) (re-matches #"^_.*" (name form)))
    (let [k (keyword (str/replace (name form) #"^_" ""))]
      `(map (fn [r#] (get r# ~k)) (get @results ~resultstype)))
    form))

(defmacro make-claim
  [claim-name & opts]
  (apply merge
         `{:name ~claim-name}
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
                        (for [verify-fn (get (first params) resultstype)]
                          ;; in verify-fn, replace mentions of :_resultKey with function that gets :resultKey from results
                          (let [result-fn (postwalk #(replace-keys resultstype %) verify-fn)]
                            `{:code '~verify-fn
                              :result (fn [] ~result-fn)})))))}))))

(defn evaluate-claim
  [run-fn claim datadir git recordsdir nthreads]
  (let [seed 1
        repetitions 100
        run-results (binding [rgen (new-seed seed)]
                      (run-with-new-record
                       run-fn (:parameters claim) datadir seed git recordsdir
                       nthreads repetitions false true true))]
    (dosync (alter results (constantly run-results)))
    (let [verifications (apply concat
                               (for [resultstype [:control :comparison :comparative]]
                                 (for [to-verify (get (:verify claim) resultstype)]
                                   (let [sw (java.io.StringWriter.)
                                         pw (java.io.PrintWriter. sw)
                                         result (binding [clojure.test/*test-out* pw
                                                          clojure.test/*report-counters* (ref clojure.test/*initial-report-counters*)]
                                                  ((:result to-verify)))]
                                     {:code (:code to-verify)
                                      :resultstype resultstype
                                      :verification-result result
                                      :verification-output (str sw)}))))]
      (doseq [ver verifications]
        (timbre/info (format "%s (%s):" (if (:verification-result ver) "PASS" "FAIL")
                             (name (:resultstype ver))))
        (timbre/info (with-out-str (pprint (:code ver))))
        (timbre/info (:verification-output ver)))
      (if (every? :verification-result verifications)
        (do
          (timbre/info (format "*** Claim \"%s\" verified.\n\n" (:name claim)))
          true)
        (do
          (timbre/info (format "!!! Claim \"%s\" not verified.\n\n" (:name claim)))
          false)))))


