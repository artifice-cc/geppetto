(ns geppetto.claim
  (:use [clojure.walk])
  (:use [geppetto.parameters :only [read-params]])
  (:use [geppetto.records :only [run-with-new-record read-archived-results]])
  (:use [geppetto.stats]))

(defn replace-keys
  [results form]
  (if (keyword? form) `(map #(get % ~form) ~results) form))

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
                   ~(vec (for [v params]
                           `{:code '~v
                             :result (fn [results#]
                                       (eval (postwalk #(replace-keys results# %) '~v)))}))}))))

(defn evaluate-claim
  [run-fn claim db-params datadir git recordsdir nthreads]
  (let [seed 1
        repetitions 10]
    (run-with-new-record
      run-fn db-params datadir seed git recordsdir
      nthreads false true repetitions)
    (let [results (read-archived-results (:recordsdir options))
          verifications (for [to-verify (:verify claim)]
                          {:code (:code to-verify)
                           :verification-result ((:result to-verify) results)})]
      (if (every? :verification-result verifications)
        (do
          (println (format "Claim \"%s\" verified." (:name claim)))
          true)
        (do
          (println (format "Claim \"%s\" not verified.\nThese failed:" (:name claim)))
          (doseq [failed (map :code (filter #(not (:verification-result %)) verifications))]
            (println (format "\t%s" failed)))
          false)))))
