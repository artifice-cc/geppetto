(ns geppetto.optimize
  (:import (java.util Date))
  (:import [java.io File])
  (:require [clojure.string :as str])
  (:use [clojure.java.io :only [file]])
  (:use [geppetto.misc :only [format-date-ms]])
  (:use [geppetto.git :only [git-meta-info]])
  (:use [geppetto.runs :only [commit-run get-raw-results]])
  (:use [geppetto.records :only [submit-results]])
  (:use [geppetto.local :only [inject-params write-results-csv]])
  (:use [geppetto.r :only [results-to-rbin]])
  (:use [geppetto.parameters :only [read-params explode-params vectorize-params]])
  (:use [geppetto.random])
  (:use [taoensso.timbre]))

(defn random-neighboring-indices
  [vparams param-indices]
  (let [chosen-key (rand-nth (filter (fn [k] (< 1 (count (get vparams k)))) (keys vparams)))
        chosen-count (count (get vparams chosen-key))
        current-index (get param-indices chosen-key)
        chosen-index (if (< (rand) 0.1)
                       ;; include this so we don't get stuck in left-right repeats
                       (rand-nth (range chosen-count))
                       (cond (= current-index 0) (inc current-index)
                             (= current-index (dec chosen-count)) (dec current-index)
                             :else (rand-nth [(inc current-index) (dec current-index)])))]
    (assoc param-indices chosen-key chosen-index)))

(defn choose-param-indices
  [vparams last-param-indices attempted-param-indices]
  (if (nil? last-param-indices)
    ;; start with random params
    (into {} (for [[k vs] (seq vparams)] [k (rand-int (count vs))]))
    (loop [i 0]
      (when (< i 100)
        (let [new-param-indices (random-neighboring-indices vparams last-param-indices)]
          (if (attempted-param-indices new-param-indices) (recur (inc i))
              new-param-indices))))))

(defn select-params-from-indices
  [vparams param-indices]
  (into {} (for [[k vs] (seq vparams)] [k (nth vs (get param-indices k))])))

(defn solution-delta
  "A negative result means there was an improvement."
  [opt-type opt-metric results-new results-old]
  (if (= :max opt-type)
    ;; maximizing, so old-new < 0 is good
    (- (get results-old opt-metric) (get results-new opt-metric))
    ;; else, minimizing, so new-old < 0 is good
    (- (get results-new opt-metric) (get results-old opt-metric))))

(defn better-than?
  [opt-type opt-metric results-new results-old strict?]
  (if strict?
    (> 0 (solution-delta opt-type opt-metric results-new results-old))
    (>= 0 (solution-delta opt-type opt-metric results-new results-old))))

(defn stopping-condition-satisfied?
  [keeps-per-temp temperature-schedule stop-cond1 stop-cond2]
  "Number of kept results is less than stop-cond1-% of
   temperature-schedule for stop-cond2 consecutive series of
   temperature-schedule steps."
  (let [recent-keeps (take stop-cond2 (sort-by first (seq keeps-per-temp)))
        keep-counts (map (fn [[t rs]] (count (filter identity rs))) recent-keeps)]
    ;; do we have enough recent temperatures to warrant this question?
    (and (<= stop-cond2 (count keeps-per-temp))
         ;; all of the last stop-cond2 keep sets need to be defficient to stop
         (every? (fn [c] (< c (* stop-cond1 temperature-schedule))) keep-counts))))

;; simulated annealing
(defn optimize-loop
  [control-params run-fn recdir opt-type opt-metric
   alpha initial-temperature temperature-schedule stop-cond1 stop-cond2 save-record?]
  (loop [best-results nil
         best-params {}
         all-results []
         kept-results []
         keeps-per-temp {} ;; keyed by temp, vals: if keeping results, then results, else nil
         last-param-indices nil
         attempted-param-indices #{}
         temperature initial-temperature
         step 1]
    (let [ps-indices (choose-param-indices control-params last-param-indices attempted-param-indices)]
      (if (or (nil? ps-indices)
              (stopping-condition-satisfied? keeps-per-temp temperature-schedule stop-cond1 stop-cond2))
        [best-results best-params all-results]
        (let [ps (assoc (select-params-from-indices control-params ps-indices)
                   :simulation (dec step) :Seed (my-rand-int 10000000))
              [control-results _ _] (run-fn false ps)
              sol-delta (when (not-empty kept-results)
                          (solution-delta opt-type opt-metric control-results (last kept-results)))
              prob (when sol-delta
                     (Math/exp (* (- (/ 1.0 temperature)) sol-delta)))
              best? (or (nil? best-results)
                        (better-than? opt-type opt-metric control-results best-results true))
              keep? (or (empty? kept-results)
                        (better-than? opt-type opt-metric control-results (last kept-results) false)
                        (< (rand) prob))
              new-best-results (if best? control-results best-results)
              new-best-params (if best? ps best-params)]
          (when save-record?
            (write-results-csv :control recdir
                               (inject-params control-results ps)))
          (info "Best?" best? "Keep?" keep? "temperature" temperature
                "step" step "solution delta" sol-delta "prob" prob
                "Best so far:" opt-metric (get new-best-results opt-metric)
                "Best params so far:" new-best-params)
          (recur new-best-results
                 new-best-params
                 (conj all-results control-results)
                 (if keep? (conj kept-results control-results) kept-results)
                 (update-in keeps-per-temp [temperature] conj (if keep? control-results nil))
                 (if keep? ps-indices last-param-indices)
                 (conj attempted-param-indices ps-indices)
                 (if (= 0 (mod step temperature-schedule)) (* alpha temperature) temperature)
                 (inc step)))))))

(defn optimize
  [run-fn params-str-or-map opt-type opt-metric
   alpha initial-temperature temperature-schedule stop-cond1 stop-cond2
   datadir seed git recordsdir nthreads repetitions upload? save-record?]
  (alter-var-root (var rgen) (constantly (new-seed seed)))
  (let [t (. System (currentTimeMillis))
        recdir (.getAbsolutePath (File. (str recordsdir "/" t)))
        ;; TODO: support comparative runs
        params (if (string? params-str-or-map) (read-params params-str-or-map)
                   ;; else, should be a map; no reason to get the params from the db
                   params-str-or-map)
        ;; don't explode params, just vectorize
        control-params (vectorize-params (:control params))
        working-directory (System/getProperty "user.dir")
        run-meta (merge {:starttime (format-date-ms t)
                         :paramid (:paramid params)
                         :datadir datadir :recorddir recdir :nthreads nthreads
                         :pwd working-directory :repetitions repetitions :seed seed
                         :hostname (.getHostName (java.net.InetAddress/getLocalHost))
                         :username (System/getProperty "user.name")}
                        (git-meta-info git working-directory))
        start-time (.getTime (Date.))]
    (info (format "Parameter space: %d possible parameters"
                  (reduce * (map count (vals control-params)))))
    (when save-record?
      (info (format "Making new directory %s ..." recdir))
      (.mkdirs (File. recdir))
      (spit (format "%s/meta.clj" recdir) (pr-str run-meta)))
    (let [[best-results best-params results]
          (optimize-loop control-params run-fn recdir opt-type opt-metric
                         alpha initial-temperature temperature-schedule
                         stop-cond1 stop-cond2 save-record?)
          run-meta-stopped (assoc run-meta :endtime (format-date-ms (. System (currentTimeMillis)))
                                  :simcount (count results))]
      (when save-record? (spit (format "%s/meta.clj" recdir) (pr-str run-meta-stopped)))
      (when (and save-record? upload?)
        (submit-results recdir))
      [best-results best-params])))
