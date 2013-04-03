(ns geppetto.stats
  (:require [incanter.stats :as is]))

(defn mean
  [vals]
  (if (empty? vals) 0.0
      (double (/ (reduce + vals) (count vals)))))

(defn linear-reg
  [xs ys]
  (is/linear-model (vec ys) (vec xs) :intercept false))

(defn paired-t-test
  [_ _ _])
