(ns geppetto.fn-test
  (:use [clojure.test])
  (:require [plumbing.graph :as graph])
  (:use [geppetto.fn]))

(deftest test-fn-params
  (let [g1 (paramfnk [x y] [a [1 2 3] b [4 5 6]] (* x y a b))
        g2 {:result (paramfnk [x y] [a [1 2 3] b [4 5 6]] (* x y a b))}
        g3 {:result g1}
        f1 (compile-graph graph/eager-compile g1)
        f2 (compile-graph graph/eager-compile g2)
        f3 (compile-graph graph/eager-compile g3)]
    (is (= (* 1 2 3 4) (f1 {:x 1 :y 2 :params {:a 3 :b 4}})))
    (is (= (* 1 2 3 4) (:result (f2 {:x 1 :y 2 :params {:a 3 :b 4}}))))
    (is (= (* 1 2 3 4) (:result (f3 {:x 1 :y 2 :params {:a 3 :b 4}}))))
    (is (= [:a :b] (fn-params g1)))
    (is (= [1 2 3] (fn-param-range g1 :a)))
    (is (= [4 5 6] (fn-param-range g1 :b)))
    (is (= [:a :b] (fn-params f1)))
    (is (= [1 2 3] (fn-param-range f1 :a)))
    (is (= [4 5 6] (fn-param-range f1 :b)))
    (is (= [:a :b] (fn-params f2)))
    (is (= [1 2 3] (fn-param-range f2 :a)))
    (is (= [4 5 6] (fn-param-range f2 :b)))
    (is (= [:a :b] (fn-params f3)))
    (is (= [1 2 3] (fn-param-range f3 :a)))
    (is (= [4 5 6] (fn-param-range f3 :b)))))

(deftest test-all-params
  (let [g1 (paramfnk [x y] [a [1 2] b [3 4]] (* x y a b))
        g2 {:x (paramfnk [y] [a [1 2]] (+ y a))
            :z (paramfnk [x y] [b [3 4]] (+ x y b))}
        f1 (compile-graph graph/eager-compile g1)
        f2 (compile-graph graph/eager-compile g2)]
    (is (= {:a [1 2] :b [3 4]} (all-params g1)))
    (is (= [{:a 1 :b 3} {:a 1 :b 4}
            {:a 2 :b 3} {:a 2 :b 4}]
           (params-to-try g1)))
    (is (= {:a [1 2] :b [3 4]} (all-params f1)))
    (is (= [{:a 1 :b 3} {:a 1 :b 4}
            {:a 2 :b 3} {:a 2 :b 4}]
           (params-to-try f1)))
    (is (= {:a [1 2] :b [3 4]} (all-params g2)))
    (is (= [{:a 1 :b 3} {:a 1 :b 4}
            {:a 2 :b 3} {:a 2 :b 4}]
           (params-to-try g2)))
    (is (= {:a [1 2] :b [3 4]} (all-params f2)))
    (is (= [{:a 1 :b 3} {:a 1 :b 4}
            {:a 2 :b 3} {:a 2 :b 4}]
           (params-to-try f2)))))
