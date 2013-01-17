(ns granary.r
  (:use [clojure.java.shell :only [sh]])
  (:require [clojure.java.io :as io])
  (:use [granary.misc]))

(def results-to-rbin-rcode
  "library(RMySQL)
con <- dbConnect(MySQL(), user='%s', password='%s', dbname='%s', host='%s')

results <- dbGetQuery(con, 'select * from results_fields where simid in (select simid from simulations where runid = %d)')

control <- data.frame()
comparison <- data.frame()
comparative <- data.frame()

for(resultstype in c('control', 'comparison', 'comparative')) {
  d <- data.frame()
  r <- results[results$resultstype == resultstype,]
  for(field in unique(r$field)) {
    tmp <- r[r$field == field,]
    if(tmp[1,'valtype'] == 'strval') {
      if(nrow(d) > 0) {
        d <- data.frame(d, f = tmp$strval);
      } else {
        d <- data.frame(f = tmp$strval);
      }
    } else if(tmp[1,'valtype'] == 'intval') {
      if(nrow(d) > 0) {
        d <- data.frame(d, f = tmp$intval);
      } else {
        d <- data.frame(f = tmp$intval);
      }
    } else {
      if(nrow(d) > 0) {
        d <- data.frame(d, f = tmp$floatval);
      } else {
        d <- data.frame(f = tmp$floatval);
      }
    }
    colnames(d)[ncol(d)] <- field
  }
  if(resultstype == 'control') {
    control <- d;
  } else if(resultstype == 'comparison') {
    comparison <- d;
  } else {
    comparative <- d;
  }
}
save(control, file='%s/%d-control.rbin', compress=TRUE);
save(comparison, file='%s/%d-comparison.rbin', compress=TRUE);
save(comparative, file='%s/%d-comparative.rbin', compress=TRUE);
")

(defn results-to-rbin
  [runid cachedir]
  (when (some #(not (. (io/file %) exists))
           (map (fn [resultstype] (format "%s/%d-%s.rbin" cachedir runid resultstype))
              ["control" "comparison" "comparative"]))
    (let [rcode (format results-to-rbin-rcode
                   @granary-dbuser @granary-dbpassword @granary-dbname @granary-dbhost
                   runid cachedir runid cachedir runid cachedir runid)
          rscript-fname (format "%s/%d-results-to-rbin.rscript" cachedir runid)]
      (with-open [writer (io/writer rscript-fname)]
        (.write writer rcode))
      (sh "/usr/bin/Rscript" rscript-fname))))
