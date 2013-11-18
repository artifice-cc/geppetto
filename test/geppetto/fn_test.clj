(ns geppetto.fn-test
  (:use [clojure.test])
  (:use [geppetto.fn]))

(deftest test-fn-params
  (let [f (paramfnk [x y] [a [1 2 3] b [4 5 6]] (* x y a b))]
    (is (= [:a :b] (fn-params f)))
    (is (= [1 2 3] (fn-param-range f :a)))
    (is (= [4 5 6] (fn-param-range f :b)))))

(deftest test-all-params
  (let [g {:x (paramfnk [y] [a [1 2]] (+ y a))
           :z (paramfnk [x y] [b [3 4]] (+ x y b))}]
    (is (= {:a [1 2] :b [3 4]} (all-params g)))
    (is (= [{:a 1 :b 3} {:a 1 :b 4}
            {:a 2 :b 3} {:a 2 :b 4}]
           (params-to-try g)))))
