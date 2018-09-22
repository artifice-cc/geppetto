(ns geppetto.parameters
  (:require [clojure.string :as str])
  (:use [korma db core])
  (:use [geppetto.models])
  (:use [geppetto.misc]))

(defn get-params
  ([paramid]
   (with-db @geppetto-db
     (update-in (first (select parameters (where {:paramid paramid})))
                [:control]
                text-field)))
  ([problem name]
   (with-db @geppetto-db
     (update-in (first (select parameters
                               (where {:name name :problem problem})
                               (order :rev :DESC) (limit 1)))
                [:control]
                text-field))))

(defn parameters-latest-rev
  [problem name]
  (with-db @geppetto-db
    (or (:rev (first (select parameters
                             (fields :rev)
                             (where {:problem problem :name name})
                             (order :rev :DESC)
                             (limit 1))))
        0)))

(defn parameters-latest
  [problem name]
  (with-db @geppetto-db
    (update-in (first (select parameters
                              (where {:problem problem :name name})
                              (order :rev :DESC)
                              (limit 1)))
               [:control]
               text-field)))

(defn parameters-latest?
  [paramid]
  (with-db @geppetto-db
    (let [{:keys [problem name rev]}
          (first (select parameters (fields :problem :name :rev)
                         (where {:paramid paramid})))]
      (= rev (parameters-latest-rev problem name)))))

;; an "update" is really an insert with a new revision
(defn update-parameters
  [params]
  (with-db @geppetto-db
    (get-insert-id
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
  (with-db @geppetto-db
    (let [problem-name-pairs (sort (set (map (fn [{:keys [problem name]}] [problem name])
                                             (select parameters (fields :problem :name)))))
          latest-params (for [[problem name] problem-name-pairs]
                          (parameters-latest problem name))]
      (reduce (fn [m ps]
                (update-in m [(if (:comparison ps) :comparative :non-comparative)] conj ps))
              {:comparative [] :non-comparative []}
              latest-params))))

(defn runs-with-parameters
  [paramid]
  (with-db @geppetto-db
    (select runs (where {:paramid paramid}) (with parameters))))

(defn delete-parameters
  [paramid]
  (with-db @geppetto-db
    (let [ps (get-params paramid)]
      (when ps
        (delete parameters (where {:problem (:problem ps) :name (:name ps)}))))))

(defn extract-problem
  [params-string]
  (first (str/split params-string #"/")))

(defn handle-params-fn
  [v]
  (cond (and (= 'range (first v))
             (every? number? (rest v)))
        (vec (eval v))
        :else v))

(defn read-params-string
  [s]
  (let [parsed (if (string? s) (read-string s) s)]
    (into {} (for [[k v] (seq parsed)]
               [k (if (list? v) (handle-params-fn v) v)]))))

(defn read-params
  [params-string]
  (let [[problem name] (if (not (re-find #"[{}]" params-string))
                         (str/split params-string #"/"))
        ;; params-string may be a clojure structure (as a string),
        ;; so if fetching the Problem/Params format did not work,
        ;; try to read it as clojure code
        params (if name (get-params problem name)
                   (try (read-string params-string)
                        (catch Exception e
                          (println "***** Error reading params string")
                          (println "*****" params-string)
                          (.printStackTrace e))))]
    (when params
      (if (:comparison params)
        (-> params
           (update-in [:control] read-params-string)
           (update-in [:comparison] read-params-string))
        (update-in params [:control] read-params-string)))))

(defn vectorize-params
  "Put every value in the input map into a vector."
  [params]
  (reduce (fn [m k] (let [v (k params)]
                      (assoc m k (if (vector? v) v [v]))))
          {} (keys params)))

(defn count-params
  [params]
  (reduce * (map (comp count second) (seq (vectorize-params params)))))

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
  (= (set (keys params1)) (set (keys params2))))
