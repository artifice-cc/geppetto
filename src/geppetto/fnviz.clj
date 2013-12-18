;; Adapted from work by:
;; Copyright Jason Wolfe and Prismatic, 2013.
;; Licensed under the EPL, same license as Clojure

(ns geppetto.fnviz
  (:require [clojure.java.shell :as shell]
            [clojure.java.io :as io]
            [clojure.string :as str])
  (:import (java.util HashSet) (java.io File))
  (:use [plumbing.core])
  (:use [plumbing.fnk.pfnk :only [input-schema]])
  (:use [geppetto.parameters])
  (:use [geppetto.fn]))

(defn double-quote [s] (str "\"" s "\""))

(defn- attribute-string [label-or-attribute-map]
  (when label-or-attribute-map
    (str "["
         (str/join "," 
                   (map (fn [[k v]] (str (name k) "=" v))
                        (if (map? label-or-attribute-map) 
                          label-or-attribute-map
                          {:label (double-quote label-or-attribute-map)})))
	 "]")))
      
(defn- walk-graph
  [g root node-key-fn node-label-fn edge-child-pair-fn
   ^HashSet visited indexer]
  (let [node-key (node-key-fn g root)
	node-map (node-label-fn g root)]
    (when-not (.contains visited node-key)
      (.add visited node-key)
      (apply str
	     (indexer node-key) (attribute-string node-map) ";\n"
	     (apply concat 
                    (for [[edge-map child] (edge-child-pair-fn root)]
                      (cons (str (indexer node-key) " -> " (indexer (node-key-fn g child)) 
                                 (attribute-string edge-map)
                                 ";\n")
                            (walk-graph g child node-key-fn node-label-fn edge-child-pair-fn
                                        visited indexer))))))))


(defn write-graphviz [folder file-stem g roots node-key-fn node-label-fn edge-child-pair-fn] 
  (let [dot-file (format "%s/%s.dot" folder file-stem)
        png-file (format "%s/%s.png" folder file-stem)
        indexer (memoize (fn [x] (double-quote (gensym))))
        vis (HashSet.)]
    (spit dot-file
          (str "strict digraph {\n"
               " rankdir = LR;\n"
               (apply str (for [root roots] (walk-graph g root node-key-fn node-label-fn
                                                        edge-child-pair-fn vis indexer)))
               "}\n"))
    (shell/sh "dot" "-Tpng" "-o" png-file dot-file)
    (format "%s.png" file-stem)))

(defn my-node-key-fn [g k] k)

(defn my-label-fn
  [g k]
  (if-let [params (fn-params (get g k))]
    (format "%s\n%s" (name k) (str/join "\n" (map str params)))
    (name k)))

(defn graphviz-el [g folder file-stem edge-list]
  (when (not-empty edge-list)
    (let [edge-map (map-vals #(map second %) (group-by first edge-list))]
      (write-graphviz
       folder file-stem g
       (set (apply concat edge-list))
       my-node-key-fn my-label-fn #(for [e (get edge-map %)] [nil e])))))

(defn graph-edges [g]
  (for [[k node] g
        :when (and (fn? node) (try (input-schema node) (catch RuntimeException _)))
        parent (keys (input-schema node))
        :when (nil? (get-in (meta node) [:params parent]))]
    [parent k]))

(defn graphviz-graph
  "Generate folder/file-stem.dot and folder/file-stem.png representing
  the nodes and edges of Graph g"
  [folder file-stem g]
  (.mkdirs (io/file folder))
  (graphviz-el g folder file-stem (graph-edges g)))
