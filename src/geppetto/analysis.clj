(ns geppetto.analysis
  (:require [incanter.stats :as stats])
  (:require [plumbing.graph :as graph])
  (:use [geppetto.fn]))

(defn try-all
  [g args default-params metric repetitions]
  (let [ps (all-fn-params-combinations g)
        f (graph/eager-compile g)
        uniq-params (set (for [params ps] (merge params default-params)))]
    (into {} (for [params uniq-params]
               (let [results (for [_ (range repetitions)]
                               (get (f (assoc args :params params))
                                    metric))]
                 [params (stats/mean results)])))))

(defn- calc-group-effect
  [results sample-size grouped-results grouped-count]
  (let [val-stats (for [[val ps] grouped-results]
                    (let [rs (map #(get results %) ps)
                          m (stats/mean rs)]
                      {:val val :n (count rs) :m m
                       :ss (reduce + (map (fn [r] (Math/pow (- r m) 2.0)) rs))}))
        within-group-var (/ (reduce + (map :ss val-stats)) (- sample-size grouped-count))
        overall-mean (stats/mean (vals results))
        between-group-var (/ (reduce + (map (fn [{:keys [n m]}]
                                              (* n (Math/pow (- m overall-mean) 2.0)))
                                            val-stats))
                             (dec grouped-count))
        f-stat (if (= 0 within-group-var) Double/NaN
                   (/ between-group-var within-group-var))]
    {:f-stat f-stat :means (into {} (map (fn [{:keys [val m]}] [val m]) val-stats))}))

(defn transform-results
  "Go from [{:result 10 :params \"{:simulation 0 :Seed 123 :Foo 3 :Bar 7}\" ...}]
   to {{:Foo 3 :Bar 7} 10 ...}"
  [results metric]
  ;; TODO: FIX for comparative runs (:control-params, :comparative-params)
  (into {} (for [rs results]
             (let [params (dissoc (read-string (:params rs)) :simulation :Seed)]
               [params (get rs metric)]))))

(defn calc-effect
  [results metric]
  (let [t-results (transform-results results metric)
        params (first (keys t-results))
        sample-size (count t-results)]
    (into {} (for [param (keys params)
                   :let [grouped-results (group-by #(get % param) (keys t-results))
                         grouped-count (count grouped-results)]
                   :when (not= 1 grouped-count)]
               [param (calc-group-effect t-results sample-size grouped-results grouped-count)]))))
