(ns geppetto.r
  (:require [clojure.string :as str])
  (:use [clojure.java.shell :only [sh]])
  (:require [clojure.java.io :as io]))

(defn results-to-rbin-rcode
  [recdir]
  (str/join "\n\n"
            (for [resultstype ["control" "comparison" "comparative"]]
              (format "%s <- data.frame()
                  %s <- read.csv(paste(\"%s-results.csv/\", file, sep=''))
                  save(%s, file=\"%s/%s.rbin\", compress=TRUE)"
                 resultstype resultstype resultstype resultstype recdir resultstype))))

(defn results-to-rbin
  [recdir]
  (try
    (let [rcode (results-to-rbin-rcode recdir)
          rscript-fname (format "%s/results-to-rbin.rscript" recdir)]
      (with-open [writer (io/writer rscript-fname)]
        (.write writer rcode))
      (sh "/usr/bin/Rscript" rscript-fname))
    (catch Exception e (println "Error running Rscript:" e))))
