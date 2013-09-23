(ns geppetto.parameters-test
  (:use clojure.test)
  (:use geppetto.parameters))

(deftest test-vectorize-params
  (is (= {:a [1] :b [3]}
         (vectorize-params {:a 1 :b 3})))
  (is (= {:a [1 2] :b [3]}
         (vectorize-params {:a [1 2] :b 3})))
  (is (= {:a [1 2] :b [3 4]}
         (vectorize-params {:a [1 2] :b [3 4]}))))

(deftest test-explode-params
  (is (= [{:a 1 :b 3} {:a 2 :b 3}]
         (explode-params {:a [1 2] :b [3]}))))

(deftest test-read-params
  (is (= {:control {:a 1 :b "str" :c 0.2}}
         (read-params "{:control {:a 1 :c 0.2 :b \"str\"}}")))
  (is (= {:control {:a [1 2 3] :b 55}}
         (read-params "{:control {:a [1 2 3] :b 55}}")))
  (is (= {:control {:a [1 2 3] :b 55}}
         (read-params "{:control {:a (range 1 4) :b 55}}")))
  (is (= {:control {:a [0.0 0.25 0.5 0.75] :b [1 2 3]}}
         (read-params "{:control {:a (range 0.0 1.0 0.25) :b (range 1 4)}}")))
  (is (= {:control {:a [0.0 0.25 0.5 0.75] :b [1 2 3]}
          :comparison {:a [0.0 0.25 0.5 0.75] :b [1 2 3]}}
         (read-params "{:control {:a (range 0.0 1.0 0.25) :b (range 1 4)} :comparison {:a (range 0.0 1.0 0.25) :b (range 1 4)}}"))))
