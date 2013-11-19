(ns geppetto.fn
  (:require [plumbing.core])
  (:require [schema.macros :as macros])
  (:require [plumbing.fnk.impl :as fnk-impl])
  (:use [geppetto.parameters]))

(defn fn-params
  [f]
  (keys (:params (meta f))))

(defn fn-param-range
  [f param]
  (get-in (meta f) [:params param]))

(defn all-fn-params
  [f-or-g]
  (if (map? f-or-g)
    (into {} (mapcat (fn [k] (for [param (fn-params (get f-or-g k))]
                               [param (fn-param-range (get f-or-g k) param)]))
                     (keys f-or-g)))
    (into {} (map (fn [k] [k (fn-param-range f-or-g k)])
                  (fn-params f-or-g)))))

(defn random-fn-params
  [f-or-g]
  (let [params (all-fn-params f-or-g)]
    (into {} (for [k (keys params)]
               [k (rand-nth (get params k))]))))

(defn all-fn-params-combinations
  [f-or-g]
  (let [params (all-fn-params f-or-g)]
    (explode-params params)))

(defmacro paramfnk
  "Usage: (paramfnk [x y] [p1 [1 2 3] p2 [true false]] (do stuff...));
   OR: (paramfnk [x y] compiled-paramfnk-graph (do stuff...)).
   In the second case, the params are inherited from compiled-paramfnk-graph,
   which must be defined with (def) and not (let) -- perhaps this can be fixed someday."
  [& args]
  (let [[name? [bind params & body]] (if (symbol? (first args))
                                       (macros/extract-arrow-schematized-element &env args)
                                       [nil args])
        params-vals (partition 2 params)
        params-meta (into {} (for [[param vals] params-vals]
                               [(keyword param) vals]))]
    (assert (vector? params))
    (assert (even? (count params)))
    (assert (apply distinct? (concat bind (keys params-meta))))
    (assert (every? coll? (map second params-vals)))
    (let [new-bind (conj bind (vec (concat [:params] (map (comp symbol name)
                                                          (keys params-meta))
                                           [:as 'params])))
          f (plumbing.fnk.impl/fnk-form name? new-bind body)]
      `(with-meta ~f (merge (meta ~f) {:params ~params-meta})))))

(defn compile-graph
  [compiler g]
  (let [f (compiler g)]
    (with-meta f (assoc (meta f) :params (all-fn-params g)))))
