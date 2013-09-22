(ns geppetto.records
  (:import [java.io File])
  (:require [clojure.string :as str])
  (:use [clojure.java.io :only [file]])
  (:use [clojure.java.shell :only [sh]])
  (:use [geppetto.local :only [run-partitions]])
  (:use [geppetto.misc :only [format-date-ms]])
  (:use [geppetto.git :only [git-meta-info]])
  (:use [geppetto.runs :only [commit-run get-raw-results]])
  (:use [geppetto.r :only [results-to-rbin]])
  (:use [geppetto.parameters :only [read-params explode-params vectorize-params]]))

(defn submit-results
  [recdir]
  (let [run-meta (read-string (slurp (format "%s/meta.clj" recdir)))]
    (println "Writing run metadata to database...")
    (commit-run run-meta)
    (println "Generating R binary data...")
    (results-to-rbin (:recorddir run-meta))
    (println "Done.")))

(defn run-with-new-record
  "Create a new folder for storing run data and execute the run."
  [run-fn params-str-or-map datadir seed git recordsdir nthreads repetitions
   upload? save-record? verifying-claim?]
  (try
    (let [t (. System (currentTimeMillis))
          recdir (.getAbsolutePath (File. (str recordsdir "/" t)))
          params (if (string? params-str-or-map) (read-params params-str-or-map)
                     ;; else, should be a map; no reason to get the params from the db
                     params-str-or-map)
          control-params (explode-params (vectorize-params (:control params)))
          comparison-params (when (:comparison params)
                              (explode-params (vectorize-params (:comparison params))))
          paired-params (when comparison-params
                          (partition 2 (interleave control-params comparison-params)))
          simcount (* (count control-params) repetitions)
          working-directory (System/getProperty "user.dir")
          run-meta (merge {:starttime (format-date-ms t)
                           :paramid (:paramid params)
                           :datadir datadir :recorddir recdir :nthreads nthreads
                           :pwd working-directory :repetitions repetitions :seed seed
                           :hostname (.getHostName (java.net.InetAddress/getLocalHost))
                           :username (System/getProperty "user.name")
                           :simcount simcount}
                          (git-meta-info git working-directory))]
      (when (and comparison-params (not= (count control-params) (count comparison-params)))
        (println "Control/comparison param counts are not equal.")
        (System/exit -1))
      (when save-record?
        (println (format "Making new directory %s ..." recdir))
        (.mkdirs (File. recdir)))
      (println (format "Running %d parameters * %d repetitions = %d simulations..."
                  (count control-params) repetitions simcount))
      (binding [*out* (if verifying-claim? (java.io.StringWriter.) *out*)]
        (doall (run-partitions run-fn run-meta (not (nil? comparison-params))
                               (if comparison-params paired-params control-params)
                               recdir nthreads save-record? repetitions)))
      (cond verifying-claim?
            (get-raw-results recdir)
            (and save-record? upload?)
            (do
              (submit-results recdir)
              (System/exit 0))
            :else
            (System/exit 0)))))
