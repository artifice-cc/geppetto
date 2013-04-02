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
  [run-fn choose-problem batch-ref problem-ref claim]
  (let [[problem-name ps] (read-params (:parameters claim))
        problem (choose-problem problem-name)
        options {:recordsdir "records-tmp" :nthreads 1
                 :datadir "data" :seed 1 :git "/usr/bin/git"
                 :upload false :save-record true :repetitions 10}]
    (dosync
     (alter batch-ref (constantly true))
     (alter problem-ref (constantly problem)))
    (run-with-new-record run-fn ps (:datadir options) (:seed options)
      (:git options) (:recordsdir options) (:nthreads options)
      (:upload options) (:save-record options) (:repetitions options))
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
