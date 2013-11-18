(ns geppetto.analysis-test
  (:use [clojure.test])
  (:use [geppetto.fn])
  (:use [geppetto.analysis]))

(def g {:sum (paramfnk [x] [a [1 2]] (+ a x))
        :prod (paramfnk [sum y] [b [3 4]] (* y sum b))})

(deftest test-try-all
  (is (= {{:a 1 :b 3} (float (* 100 (+ 1 11) 5))
          {:a 1 :b 4} (float (* 100 (+ 1 11) 5))
          {:a 2 :b 3} (float (* 100 (+ 2 11) 5))
          {:a 2 :b 4} (float (* 100 (+ 2 11) 5))}
         (try-all g {:b 5 :x 11 :y 100} :prod 1)))
  (is (= {{:a 1 :b 3} (float (* 100 (+ 1 11) 5))
          {:a 1 :b 4} (float (* 100 (+ 1 11) 5))
          {:a 2 :b 3} (float (* 100 (+ 2 11) 5))
          {:a 2 :b 4} (float (* 100 (+ 2 11) 5))}
         (try-all g {:b 5 :x 11 :y 100} :prod 10))))
