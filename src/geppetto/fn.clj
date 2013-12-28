(ns geppetto.fn
  (:require [clojure.walk :as walk])
  (:require [clojure.tools.macro :as macro])
  (:require [plumbing.core])
  (:require [schema.macros :as macros])
  (:require [plumbing.fnk.schema :as schema])
  (:require [plumbing.fnk.impl :as fnk-impl])
  (:require [plumbing.fnk.pfnk :as pfnk])
  (:require [clojure.core.cache :as cache])
  (:use [geppetto.parameters]))

(defn fn-params
  [f]
  (keys (:params (meta f))))

(defn fn-param-range
  [f param]
  (get-in (meta f) [:params param]))

(defn all-fn-params
  [f-or-g]
  (cond (map? f-or-g)
        (into {} (mapcat (fn [k] (for [param (fn-params (get f-or-g k))]
                                   [param (fn-param-range (get f-or-g k) param)]))
                         (keys f-or-g)))
        (fn? f-or-g)
        (into {} (map (fn [k] [k (fn-param-range f-or-g k)])
                      (fn-params f-or-g)))
        :else {}))

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
                               [(keyword param) `(vec ~vals)]))]
    (assert (vector? params))
    (assert (even? (count params)))
    (assert (apply distinct? (concat bind (keys params-meta))))
    (assert (every? coll? (map second params-vals)))
    (let [new-bind (conj bind (vec (concat [:params] (map (comp symbol name)
                                                          (keys params-meta))
                                           [:as 'params])))
          f (plumbing.fnk.impl/fnk-form name? new-bind body)]
      `(with-meta ~f (merge (meta ~f) {:params ~params-meta :bindings '~bind})))))

(defn fnkc-form
  [fn-name bind body]
  (let [{:keys [map-sym body-form input-schema]} (plumbing.fnk.impl/letk-input-schema-and-body-form
                                                  bind [] `(do ~@body))
        schema [input-schema (or (:output-schema (meta bind))
                                 (plumbing.fnk.schema/guess-expr-output-schema (last body)))]]
    (pfnk/fn->fnk
     `(fn ~fn-name
        [~map-sym]
        (let [cache-key# {:fn-name (keyword '~fn-name) :args (dissoc ~map-sym :cache)}]
          (plumbing.fnk.schema/assert-iae (= (class (:cache ~map-sym)) clojure.lang.Atom)
                                          ":cache key in input map is not an atom.")
          (plumbing.fnk.schema/assert-iae ((supers (class @(:cache ~map-sym))) clojure.core.cache.CacheProtocol)
                                          ":cache key in input map is not a clojure.core.cache object.")
          (plumbing.fnk.schema/assert-iae (map? ~map-sym) "fnk called on non-map: %s" ~map-sym)
          (if (clojure.core.cache/has? @(:cache ~map-sym) cache-key#)
            (swap! (:cache ~map-sym) clojure.core.cache/hit cache-key#)
            (swap! (:cache ~map-sym) clojure.core.cache/miss cache-key#
                   ~body-form))
          (clojure.core.cache/lookup @(:cache ~map-sym) cache-key#)))
     schema)))

(defmacro fnkc
  [& args]
  (assert (symbol? (first args)))
  (let [[fn-name [bind & body]] [(first args) (next args)]
        new-bind (conj bind 'cache)]
    (fnkc-form fn-name new-bind body)))

(defmacro paramfnkc
  [& args]
  (assert (symbol? (first args)))
  (let [[fn-name [bind params & body]] (macros/extract-arrow-schematized-element &env args)
        params-vals (partition 2 params)
        params-meta (into {} (for [[param vals] params-vals]
                               [(keyword param) `(vec ~vals)]))]
    (assert (vector? params))
    (assert (even? (count params)))
    (assert (apply distinct? (concat bind (keys params-meta))))
    (assert (every? coll? (map second params-vals)))
    (let [new-bind (conj bind (vec (concat [:params] (map (comp symbol name)
                                                          (keys params-meta))
                                           [:as 'params]))
                         'cache)
          f (fnkc-form fn-name new-bind body)]
      `(with-meta ~f (merge (meta ~f) {:params ~params-meta :bindings '~bind})))))

(defn compile-graph
  [compiler g]
  (let [f (compiler g)]
    (with-meta f (assoc (meta f) :params (all-fn-params g)))))

(defmacro fn-with-params
  [bind & body]
  (let [symbols (atom #{})
        excluded (set (conj bind 'params))
        param-extractor (fn [form]
                          (if (and (symbol? form) (not (excluded form))
                                   (not (special-symbol? form))
                                   (or (resolve form) (get &env form)))
                            (swap! symbols conj form))
                          form)]
    (walk/postwalk param-extractor (macro/mexpand-all body))
    (let [syms @symbols
          new-bind [{:keys (vec (conj bind 'params))}]]
      `(with-meta (fn ~new-bind ~@body)
         {:bindings '~bind
          :params (reduce merge (map (fn [~'sym] (geppetto.fn/all-fn-params ~'sym)) ~syms))}))))
