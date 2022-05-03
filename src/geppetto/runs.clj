(ns geppetto.runs
  (:require [clojure.string :as str])
  (:require [clojure.set :as set])
  (:use [korma db core])
  (:use [clojure-csv.core :only [parse-csv]])
  (:use [clojure.java.io :only [reader file]])
  (:use [geppetto.models])
  (:use [geppetto.misc]))

(defn commit-run
  [run-meta]
  (with-db @geppetto-db
    (get-insert-id (insert runs (values [run-meta])))))

(defn get-run
  [runid]
  (with-db @geppetto-db
    (let [run (first (select runs
                             (with parameters)
                             (where {:runid runid})
                             (fields :runid :starttime :endtime :username
                                     :seed :nthreads :repetitions :simcount
                                     :pwd :hostname :recorddir :datadir :project
                                     :commit :commitdate :commitmsg :branch
                                     :runs.paramid :parameters.name
                                     :parameters.problem :parameters.description
                                     :parameters.control :parameters.comparison)))]
      (update-in run [:control] text-field))))

(defn list-runs
  []
  (with-db @geppetto-db
    (map #(update-in % [:control] text-field)
         (select runs (with parameters)))))

(defn delete-run
  [runid]
  (with-db @geppetto-db
    (delete runs (where {:runid runid}))))

(defn list-projects
  []
  (with-db @geppetto-db
    (sort (set (filter identity (map :project (select runs (fields :project))))))))

(defn set-project
  [runid project]
  (with-db @geppetto-db
    (update runs (set-fields {:project project}) (where {:runid runid}))))

(defn gather-results-fields
  [runid resultstype]
  (let [recorddir (:recorddir (get-run runid))
        csv-file (format "%s/%s-results.csv" recorddir (name resultstype))
        first-line (try (first (with-open [rdr (reader csv-file)] (line-seq rdr)))
                        (catch Exception e))]
    (if first-line
      (map keyword (sort (str/split first-line #",")))
      [])))

(defn read-csv
  [lines]
  (let [headers (map keyword (str/split (first lines) #","))]
    (doall
     (for [line (parse-csv (str/join "\n" (rest lines)))]
       (let [data (map #(cond (re-matches #"^(true|false)$" %) (Boolean/parseBoolean %)
                            (re-matches #"^-?\d+\.\d+E?-?\d*$" %) (Double/parseDouble %)
                            (re-matches #"^\d+$" %) (Integer/parseInt %)
                            :else %)
                     line)]
         (apply hash-map (interleave headers data)))))))

(defn get-results-from-recorddir
  [recorddir resultstype selected-fields]
  (let [fields (if selected-fields
                 (set/union (set selected-fields)
                            #{:params :control-params :comparison-params}))
        csv-file (format "%s/%s-results.csv" recorddir (name resultstype))]
    (try (let [results (read-csv (str/split (slurp csv-file) #"\n"))]
           (if fields (map (fn [row] (select-keys row fields)) results)
             results))
         (catch Exception _ nil))))

(defn get-results
  [runid resultstype selected-fields]
  (let [run (get-run runid)
        recorddir (:recorddir run)]
    (get-results-from-recorddir recorddir resultstype selected-fields)))

(defn get-raw-results
  "Get results without associating in simid and control-params/comparison-params."
  [recorddir]
  (into {} (for [resultstype [:control :comparison :comparative]]
             (when-let [r (get-results-from-recorddir recorddir resultstype nil)]
               [resultstype r]))))

(defn find-abandoned-runs
  [recorddir]
  (let [existing-record-folders (map #(.getName %) (.listFiles (file recorddir)))
        existing-run-folders (set (map #(second (re-find #".*[\\/](\d+)$" (:recorddir %))) (list-runs)))]
    (filter #(not (existing-run-folders %)) existing-record-folders)))
