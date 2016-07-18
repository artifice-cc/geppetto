(ns geppetto.stats)

(defn mean
  [vals]
  (if (empty? vals) 0.0
      (double (/ (reduce + vals) (count vals)))))
