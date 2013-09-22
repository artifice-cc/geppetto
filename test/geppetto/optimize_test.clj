(ns geppetto.optimize-test
  (:use [clojure.test])
  (:use [geppetto.optimize]))

(deftest test-select-params-from-indices
  (is (= {:Foo 1 :Bar 2 :Baz 5} (select-params-from-indices {:Foo [1 2 3] :Bar [0 1 2] :Baz [5]}
                                                            {:Foo 0 :Bar 2 :Baz 0}))))

(deftest test-optimize
  (let [run-fn (fn [comparative? params] [{:result (* (:Foo params) (:Bar params) (:Baz params))}])
        results (optimize run-fn {:control {:Foo [2 3 5] :Bar [7 11] :Baz 13 :Quux (vec (repeat 10000 1))}}
                          :max :result 0.95 10.0 10 0.03 5
                          "data" 0 "git" "test/records" 1 1 false false)]
    ;; best is 5 * 11 * 13 = 715
    (is (= results {:result 715}))))
