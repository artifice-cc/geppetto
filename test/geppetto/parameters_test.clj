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
