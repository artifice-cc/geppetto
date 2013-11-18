(ns geppetto.analysis-test
  (:use [clojure.test])
  (:use [geppetto.fn])
  (:use [geppetto.analysis]))

(def g {:sum (paramfnk [x] [a [1 2]] (+ a x))
        :prod (paramfnk [sum y] [b [3 4]] (* y sum b))})

(deftest test-try-all
  (is (= (set [[{:a 2 :b 5} (float (* 100 (+ 2 11) 5))]])
         (set (try-all g {:x 11 :y 100} {:a 2 :b 5} :prod 1))))
  (is (= (set [[{:a 2 :b 3} (float (* 100 (+ 2 11) 3))]
               [{:a 2 :b 4} (float (* 100 (+ 2 11) 4))]])
         (set (try-all g {:x 11 :y 100} {:a 2} :prod 1))))
  (is (= (set [[{:a 1 :b 3} (float (* 100 (+ 1 11) 3))]
               [{:a 1 :b 4} (float (* 100 (+ 1 11) 4))]
               [{:a 2 :b 3} (float (* 100 (+ 2 11) 3))]
               [{:a 2 :b 4} (float (* 100 (+ 2 11) 4))]])
         (set (try-all g {:x 11 :y 100} {} :prod 10)))))
