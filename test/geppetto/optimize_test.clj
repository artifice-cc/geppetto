(ns geppetto.optimize-test
  (:use [clojure.test])
  (:use [geppetto.optimize])
  (:use [geppetto.test-fixtures]))

(use-fixtures :each quiet-mode)

(deftest test-select-params-from-indices
  (is (= {:Foo 1 :Bar 2 :Baz 5} (select-params-from-indices {:Foo [1 2 3] :Bar [0 1 2] :Baz [5]}
                                                            {:Foo 0 :Bar 2 :Baz 0}))))

(deftest test-random-params-true-false
  (let [vparams {:Foo [true false] :Bar [true false] :Baz (vec (range 1000))}
        params (loop [ps #{}
                      last-param-indices nil
                      attempted-param-indices #{}]
                 (let [ps-indices (choose-param-indices
                                   vparams last-param-indices attempted-param-indices)]
                   (if (nil? ps-indices) ps
                       (recur (conj ps (select-params-from-indices vparams ps-indices))
                              ps-indices
                              (conj attempted-param-indices ps-indices)))))]
    (is (some #(= {:Foo true :Bar true} (select-keys % [:Foo :Bar])) params))
    (is (some #(= {:Foo false :Bar true} (select-keys % [:Foo :Bar])) params))
    (is (some #(= {:Foo true :Bar false} (select-keys % [:Foo :Bar])) params))
    (is (some #(= {:Foo false :Bar false} (select-keys % [:Foo :Bar])) params))))

(deftest test-optimize
  (let [run-fn (fn [comparative? params] [{:result (* (:Foo params) (:Bar params) (:Baz params))}])
        [best-results best-params]
        (optimize run-fn {:control {:Foo [2 3 5] :Bar [7 11] :Baz 13 :Quux (vec (repeat 10000 1))}}
                  :max :result 0.95 10.0 10 0.03 5
                  "data" 0 "git" "" 1 1 false false)]
    ;; best is 5 * 11 * 13 = 715
    (is (= {:Foo 5 :Bar 11 :Baz 13 :Quux 1} (select-keys best-params [:Foo :Bar :Baz :Quux])))
    (is (= best-results {:result 715}))))
