(ns geppetto.local
  (:import (java.util Date))
  (:require [clojure.string :as str])
  (:use [clojure.java.io :as io :only [writer file]])
  (:use [clojure-csv.core :only [write-csv]])
  (:use [geppetto.misc])
  (:use [geppetto.random])
  (:use [taoensso.timbre])
  (:require [clansi.core :as clansi]))

(defn format-time
  [seconds]
  (let [hours (int (/ seconds 3600.0))
	mins (int (/ (rem seconds 3600) 60.0))
	secs (rem seconds 60)]
    (format "%02dh %02dm %02ds" hours mins secs)))

(defn print-progress
  [elapsed finished total]
  (let [remaining (- total finished)
        avgtime (/ elapsed finished)
        expected (* remaining avgtime)
        wallexpected (.toString (Date. (long (+ expected (.getTime (Date.))))))]
    (println (clansi/style
              (format "*** Done %d/%d\t Elapsed: %s\t Remaining: %s\t Ending %s\n"
                      finished
                      total
                      (format-time (int (/ elapsed 1000.0)))
                      (format-time (int (/ expected 1000.0)))
                      wallexpected)
              :bright :blue))))

;; keep track of progress
(def progress (ref 0))

(defn write-results-csv
  [filename results]
  (dosync
   (let [new-file? (not (. (io/file filename) exists))
         row (map (fn [field] (get results field))
                  (sort (keys results)))]
     (with-open [writer (io/writer filename :append true)]
       (when new-file?
         (.write writer (write-csv [(map name (sort (keys results)))])))
       (.write writer (write-csv [(map str row)]))))))

(defn inject-params
  [result control-params]
  (merge (assoc result :params (pr-str control-params))
         control-params))

(defn inject-control-params  
  [results [control-params comparison-params]]
  (map (fn [rs] (merge (assoc rs :control-params (pr-str control-params)
                              :comparison-params (pr-str comparison-params))
                       control-params))
       results))

(defn inject-comparison-params
  [results [control-params comparison-params]]
  (map (fn [rs] (merge (assoc rs :control-params (pr-str control-params)
                              :comparison-params (pr-str comparison-params))
                       comparison-params))
       results))

(defn prefix-params
  [prefix params]
  (zipmap (map (fn [k] (keyword (format "%s%s" prefix (name k)))) (keys params))
          (vals params)))

(defn inject-comparative-params
  [results [control-params comparison-params]]
  (let [cont-params (prefix-params "Cont" (dissoc control-params :simulation))
        comp-params (prefix-params "Comp" (dissoc comparison-params :simulation))]
    (map (fn [rs] (merge rs cont-params comp-params)) results)))

(defn run-partition
  [run-fn comparative? recdir params start-time sim-count save-record?]
  (loop [ps params]
    (when (< 0 @progress)
      (print-progress (- (.getTime (Date.)) start-time) @progress sim-count))
    (if (not-empty ps)
      (if comparative?
        (let [[control-results comparison-results comparative-results]
              (run-fn comparative? (first ps))
              control-results2 (inject-control-params control-results (first ps))
              comparison-results2 (inject-comparison-params comparison-results (first ps))
              comparative-results2 (inject-comparative-params comparative-results (first ps))]
          (when save-record?
            (doseq [rs control-results2]
              (write-results-csv (format "%s/control-results.csv" recdir) rs))
            (doseq [rs comparison-results2]
              (write-results-csv (format "%s/comparison-results.csv" recdir) rs))
            (doseq [rs comparative-results2]
              (write-results-csv (format "%s/comparative-results.csv" recdir) rs)))
          (dosync
           (alter progress inc))
          (recur (rest ps)))
        (let [control-results (map #(inject-params % (first ps))
                                   (run-fn comparative? (first ps)))]
          (when save-record?
            (doseq [rs control-results]
              (write-results-csv (format "%s/control-results.csv" recdir) rs)))
          (dosync (alter progress inc))
          (recur (rest ps)))))))

(defn run-partitions
  [run-fn run-meta comparative? params recdir nthreads save-record? repetitions]
  (when save-record? (spit (format "%s/meta.clj" recdir) (pr-str run-meta)))
  (dosync (alter progress (constantly 0)))
  (let [start-time (.getTime (Date.))
        sim-count (* repetitions (count (set params)))
        seeds (repeatedly repetitions #(my-rand-int 10000000))
        seeded-params (mapcat (fn [pp] (for [s seeds]
                                        (if comparative?
                                          (map (fn [p] (assoc p :Seed s)) pp)
                                          (assoc pp :Seed s))))
                              params)
        numbered-params (map (fn [i]
                             (if comparative?
                               (map #(assoc % :simulation i) (nth seeded-params i))
                               (assoc (nth seeded-params i) :simulation i)))
                           (range (count seeded-params)))
        partitions (partition-all (int (Math/ceil (/ (count numbered-params) nthreads)))
                                  (my-shuffle numbered-params))
        workers (for [part partitions]
                  (future (run-partition run-fn comparative? recdir part
                                         start-time sim-count save-record?)))]
    (doall (pmap (fn [w] @w) workers))
    (let [run-meta-stopped (assoc run-meta :endtime
                                  (format-date-ms (. System (currentTimeMillis))))]
      (when save-record? (spit (format "%s/meta.clj" recdir) (pr-str run-meta-stopped))))))
