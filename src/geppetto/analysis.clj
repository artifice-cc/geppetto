(ns geppetto.analysis
  (:require [incanter.stats :as stats])
  (:require [plumbing.graph :as graph])
  (:use [geppetto.fn]))

(defn try-all
  [g args default-params metric repetitions]
  (let [ps (params-to-try g)
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
        f-stat (/ between-group-var within-group-var)]
    {:f-stat f-stat :means (into {} (map (fn [{:keys [val m]}] [val m]) val-stats))}))

(defn calc-effect
  [g filename metric repetitions]
  (let [params (all-params g)
        results (try-all g filename metric repetitions)
        sample-size (count results)
        param-stats (into {} (for [param (keys params)
                                   :let [grouped-results (group-by #(get % param) (keys results))
                                         grouped-count (count grouped-results)]
                                   :when (not= 1 grouped-count)]
                               [param (calc-group-effect results sample-size
                                                         grouped-results grouped-count)]))]
    param-stats))
