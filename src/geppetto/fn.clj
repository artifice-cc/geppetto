(ns geppetto.fn
  (:require [clojure.set :as set])
  (:require [clojure.walk :as walk])
  (:require [clojure.tools.macro :as macro])
  (:require [plumbing.core])
  (:require [schema.macros :as macros])
  (:require [plumbing.fnk.schema :as schema])
  (:require [plumbing.fnk.impl :as fnk-impl])
  (:require [plumbing.fnk.pfnk :as pfnk])
  (:require [clojure.core.cache :as cache])
  (:require [loom.graph :as loom])
  (:require [loom.alg :as loom.alg])
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
   OR: (paramfnk [x y] [p1 [1 2 3] p2 [true false]] :when {:cond (boolean-fn...) :else default-val} (do stuff...));
   OR: (paramfnk [x y] compiled-paramfnk-graph (do stuff...)).
   In the last case, the params are inherited from compiled-paramfnk-graph,
   which must be defined with (def) and not (let) -- perhaps this can be fixed someday."
  [& args]
  (let [[name? [bind params & body-tmp]] (if (symbol? (first args))
                                           (macros/extract-arrow-schematized-element &env args)
                                           [nil args])
        [when-map body] (if (= :when (first body-tmp))
                           [(second body-tmp) (rest (rest body-tmp))]
                           [nil body-tmp])
        when-body (if when-map
                    `((if ~(:cond when-map) (do ~@body) ~(:else when-map)))
                    body)
        params-vals (partition 2 params)
        params-meta (into {} (for [[param vals] params-vals]
                               [(keyword param) `(vec ~vals)]))]
    (assert (vector? params))
    (assert (even? (count params)))
    (assert (apply distinct? (concat bind (keys params-meta))))
    (assert (every? coll? (map second params-vals)))
    (let [new-bind (conj bind
                         (vec (concat [:params] (map (comp symbol name)
                                                     (keys params-meta))
                                      [:as 'params])))
          [schematized-bind new-body] (macros/extract-arrow-schematized-element
                                        &env (vec (apply conj [new-bind] when-body)))
          f (plumbing.fnk.impl/fnk-form &env name? schematized-bind new-body &form)]
      `(let [func# ~f]
         (vary-meta func# merge {:params ~params-meta
                                 :bindings '~bind
                                 :when '~when-map})))))

(defmacro paramfnk-hashed
  [& args]
  (let [[name? [bind params & body-tmp]] (if (symbol? (first args))
                                           (macros/extract-arrow-schematized-element &env args)
                                           [nil args])
        [when-map body] (if (= :when (first body-tmp))
                           [(second body-tmp) (rest (rest body-tmp))]
                           [nil body-tmp])
        when-body (if when-map
                    `((if ~(:cond when-map) (do ~@body) ~(:else when-map)))
                    body)
        delay-bind-let (vec (mapcat (fn [b] [b `(if (delay? ~b) @~b ~b)]) bind))
        delay-body `((let ~delay-bind-let ~@when-body))
        delay-check (map (fn [b] `(or (delay? ~b) (= (get ~'hashes ~(keyword b)) (hash ~b)))) bind)
        hash-body `((if (not-empty ~'hashes)
                      (if (and ~@delay-check)
                        ;; no need to compute this, unless some downstream fn needs this value, which presumably
                        ;; hasn't been provided so must be computed
                        (delay ~@delay-body)
                        ~@delay-body)
                      (do ~@when-body)))
        params-vals (partition 2 params)
        params-meta (into {} (for [[param vals] params-vals]
                               [(keyword param) `(vec ~vals)]))]
    (assert (vector? params))
    (assert (even? (count params)))
    (assert (apply distinct? (concat bind (keys params-meta))))
    (assert (every? coll? (map second params-vals)))
    (let [new-bind (conj bind
                         (vec (concat [:params] (map (comp symbol name)
                                                     (keys params-meta))
                                      [:as 'params]))
                         'hashes)
          [schematized-bind new-body] (macros/extract-arrow-schematized-element
                                        &env (vec (apply conj [new-bind] hash-body)))
          f (plumbing.fnk.impl/fnk-form &env name? schematized-bind new-body &form)]
      `(let [func# ~f]
         (vary-meta func# merge {:params ~params-meta
                                 :bindings '~bind
                                 :when '~when-map})))))

(defn fnkc-form
  [env fn-name bind body]
  (let [[schematized-bind new-body] (macros/extract-arrow-schematized-element env
                                                                              (vec (apply conj [bind] body)))
        {:keys [map-sym body-form input-schema]} (plumbing.fnk.impl/letk-input-schema-and-body-form
                                                  env schematized-bind [] `(do ~@new-body))
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
    (fnkc-form &env fn-name new-bind body)))

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
          f (fnkc-form &env fn-name new-bind body)]
      `(let [func# ~f]
         (with-meta func# (merge (meta func#) {:params ~params-meta :bindings '~bind}))))))

(defn compile-graph
  [compiler g]
  (let [f (compiler g)]
    (vary-meta f assoc :params (all-fn-params g))))

(defmacro fn-with-params
  [bind & body]
  (let [symbols (atom #{})
        excluded (set (conj bind 'params))
        param-extractor (fn [form]
                          (if (and (symbol? form) (not (excluded form))
                                   (not (special-symbol? form))
                                   (or (resolve form) (get &env form)))
                            ;; use (or (resolve form) form) because
                            ;; without it, we can have cases where,
                            ;; e.g., clojure.core/count and count
                            ;; (unqualified) can both be added to the
                            ;; set of symbols, but when the macro
                            ;; output is actually compiled, we get
                            ;; clojure.core/count and
                            ;; clojure.core/count which produces an
                            ;; error, since these are both in a set
                            (swap! symbols conj (or (resolve form) form)))
                          form)]
    (walk/postwalk param-extractor (macro/mexpand-all body))
    (let [syms @symbols
          new-bind [{:keys (vec (conj bind 'params))}]]
      `(with-meta (fn ~new-bind ~@body)
         {:bindings '~bind
          :params (reduce merge (map (fn [~'sym] (geppetto.fn/all-fn-params ~'sym)) ~syms))}))))

(defn graph-edges [g]
  (for [[k node] g
        :when (and (fn? node) (try (pfnk/input-schema node) (catch RuntimeException _)))
        parent (keys (pfnk/input-schema node))
        :when (keyword? parent)]
    [parent k]))

(defn generate-fn-path
  "Given a function graph and a goal (e.g., :fulltext), and a set of inputs we already have,
  determine what other inputs are required to arrive at the goal."
  [g goals have-set]
  {:pre [(and (set? goals) (set? have-set))]}
  (let [goals2 (filter (fn [g] (not (have-set g))) goals)]
    (if (empty? goals2)
      {:need #{}
       :path []}
      (let [g-reverse (apply loom/digraph (map reverse (graph-edges g)))
            ;; insert dummy node that points to goals so we can easily find a path for multiple goals
            root (gensym)
            g2 (apply loom/add-edges g-reverse (map (fn [goal] [root goal]) goals2))
            need (atom #{})
            ;; note, (set) will force evaluation (non-lazy), which is needed since we are using an atom
            traversal (set (loom.alg/bf-traverse g2 root
                                                   :when (fn [n predecessor depth]
                                                           (if (or (have-set n) (empty? (loom/successors g2 n)))
                                                             (do (swap! need conj n)
                                                                 false)
                                                             true))))]
        {:need @need
         ;; traversal was breadth-first, which isn't right, so use traveral to see which nodes we hit from a topological sort
         :path (filter (fn [n] (traversal n)) (reverse (loom.alg/topsort g-reverse)))}))))

