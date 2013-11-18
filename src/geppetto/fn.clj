(ns geppetto.fn
  (:use [plumbing.core])
  (:require [schema.macros :as macros])
  (:require [plumbing.fnk.impl :as fnk-impl])
  (:use [geppetto.parameters]))

(defmacro paramfnk
  "Usage: (paramfnk [x y] [p1 [1 2 3] p2 [true false]] (do stuff...))"
  [& args]
  (let [[name? [bind params & body]] (if (symbol? (first args))
                                       (macros/extract-arrow-schematized-element &env args)
                                       [nil args])
        params-vals (partition 2 params)
        params-meta (into {} (for [[param vals] params-vals]
                               [(keyword param) vals]))]
    (assert (apply distinct? (concat bind (map first params-vals))))
    (assert (vector? params))
    (assert (even? (count params)))
    (assert (every? (comp coll? second) params-vals))
    (let [f (fnk-impl/fnk-form name? (vec (concat bind (map first params-vals))) body)]
      `(with-meta ~f (merge (meta ~f) {:params ~params-meta})))))

(defn fn-params
  [f]
  (keys (:params (meta f))))

(defn fn-param-range
  [f param]
  (get-in (meta f) [:params param]))

(defn all-params
  [g]
  (into {} (mapcat (fn [k] (for [param (fn-params (get g k))]
                             [param (fn-param-range (get g k) param)]))
                   (keys g))))

(defn params-to-try
  [g]
  (let [params (all-params g)]
    (explode-params params)))
