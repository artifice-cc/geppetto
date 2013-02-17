(ns granary.parameters
  (:require [clojure.string :as str])
  (:use [korma.core])
  (:use [granary.models])
  (:use [granary.misc]))

(defn get-params
  ([paramid]
     (first (with-db @granary-db
              (select parameters (where {:paramid paramid})))))
  ([problem name]
     (first (with-db @granary-db
              (select parameters
                      (where {:name name :problem problem})
                      (order :rev :DESC) (limit 1))))))

(defn parameters-latest-rev
  [problem name]
  (or (:rev (first (with-db @granary-db
                     (select parameters
                             (fields :rev)
                             (where {:problem problem :name name})
                             (order :rev :DESC)
                             (limit 1)))))
      0))

(defn parameters-latest
  [problem name]
  (first (with-db @granary-db
           (select parameters
                   (where {:problem problem :name name})
                   (order :rev :DESC)
                   (limit 1)))))

(defn parameters-latest?
  [paramid]
  (let [{:keys [problem name rev]}
        (first (with-db @granary-db
                 (select parameters (fields :problem :name :rev)
                         (where {:paramid paramid}))))]
    (= rev (parameters-latest-rev problem name))))

;; an "update" is really an insert with a new revision
(defn update-parameters
  [params]
  (:generated_key
   (with-db @granary-db
     (insert parameters (values [{:problem (:problem params)
                                  :name (:name params)
                                  :rev (inc (parameters-latest-rev (:problem params)
                                                                   (:name params)))
                                  :comparison (when (not-empty (:comparison params))
                                                (:comparison params))
                                  :control (:control params)
                                  :description (:description params)}])))))

(defn new-parameters
  [params]
  (update-parameters params))

(defn list-parameters
  []
  (let [problem-name-pairs (sort (set (map (fn [{:keys [problem name]}] [problem name])
                                         (with-db @granary-db
                                           (select parameters (fields :problem :name))))))
        latest-params (for [[problem name] problem-name-pairs]
                        (parameters-latest problem name))]
    (reduce (fn [m ps]
         (update-in m [(if (:comparison ps) :comparative :non-comparative)] conj ps))
       {:comparative [] :non-comparative []}
       latest-params)))

(defn runs-with-parameters
  [paramid]
  (with-db @granary-db
    (select runs (where {:paramid paramid}))))

(defn delete-parameters
  [paramid]
  (with-db @granary-db
    (let [ps (get-params paramid)]
      (when ps
        (delete parameters (where {:problem (:problem ps) :name (:name ps)}))))))

(defn read-params
  [params-string]
  (let [[problem name] (str/split params-string #"/")
        params (get-params problem name)]
    (when params
      [problem (if (:comparison params)
                 (-> params
                    (update-in [:control] read-string)
                    (update-in [:comparison] read-string))
                 (update-in params [:control] read-string))])))

(defn vectorize-params
  [params]
  (reduce (fn [m k] (let [v (k params)]
                      (assoc m k (if (vector? v) v [v]))))
          {} (keys params)))

(defn explode-params
  "Want {:Xyz [1 2 3], :Abc [3 4]} to become [{:Xyz 1, :Abc 3}, {:Xyz 2, :Abc 4}, ...]"
  [params]
  (when (not-empty params)
    (if (= 1 (count params))
      (for [v (second (first params))]
        {(first (first params)) v})
      (let [p (first params)
            deeper (explode-params (rest params))]
        (flatten (map (fn [v] (map #(assoc % (first p) v) deeper)) (second p)))))))

(defn params-pairable?
  [params1 params2]
  (= (set (keys params1) (keys params2))))
