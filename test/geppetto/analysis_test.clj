(ns geppetto.analysis-test
  (:use [clojure.test])
  (:use [geppetto.fn])
  (:use [geppetto.analysis])
  (:use [geppetto.runs])
  (:use [geppetto.records])
  (:use [geppetto.parameters])
  (:use [geppetto.test-fixtures])
  (:require [plumbing.graph :as graph]))

(use-fixtures :each setup-random-seed travis-mysql-db quiet-mode)

(def g (graph/graph {:sum (paramfnk [x] [a [1 2]] (+ a x))
                     :prod (paramfnk [sum y] [b [3 4]] (* y sum b))}))

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

(deftest test-calc-effect
  (let [f (compile-graph graph/eager-compile g)]
    (is (= {:a {:f-stat 9.8, :means {1 35.0, 2 70.0}}
            :b {:f-stat 0.36, :means {3 45.0, 4 60.0}}}
           (calc-effect
            (for [params (all-fn-params-combinations f)]
              (assoc (f {:x 0 :y 10 :params params})
                :params (pr-str params)))
            :prod)))))

(def run-fn (fn [comparative? params]
              [{:early-result (+ (:Foo params) (:Bar params))
                :result (* (:Foo params) (:Bar params))
                :some-string "baz"}]))

(deftest test-calc-effect-run
  (new-parameters {:problem "Testing"
                   :name "test-runs"
                   :control (pr-str {:Foo [2 3 5] :Bar [7 11]})
                   :description "testing params"})
  (let [runid (run-with-new-record
               run-fn "Testing/test-runs" "data" 0 "git" "test/records" 1 1 true true false)
        results (get-results runid :control [:early-result :result])
        results-all (get-results runid :control nil)]
    (is (= (calc-effect results :result)
           {:Bar {:f-stat 1.3445378151260503
                  :means {7 23.333333333333332, 11 36.666666666666664}}
            :Foo {:f-stat 3.730263157894737
                  :means {2 18.0, 3 27.0, 5 45.0}}}))
    (is (= (calc-effect results)
           {:early-result {:Bar {:f-stat 10.285714285714285,
                                 :means {7 10.333333333333334, 11 14.333333333333334}},
                           :Foo {:f-stat 0.5833333333333334,
                                 :means {2 11.0, 3 12.0, 5 14.0}}},
            :result {:Bar {:f-stat 1.3445378151260503,
                           :means {7 23.333333333333332, 11 36.666666666666664}},
                     :Foo {:f-stat 3.730263157894737,
                           :means {2 18.0, 3 27.0, 5 45.0}}}}))
    (is (= (calc-effect results-all)
           {:early-result {:Bar {:f-stat 10.285714285714285,
                                 :means {7 10.333333333333334, 11 14.333333333333334}},
                           :Foo {:f-stat 0.5833333333333334,
                                 :means {2 11.0, 3 12.0, 5 14.0}}},
            :result {:Bar {:f-stat 1.3445378151260503,
                           :means {7 23.333333333333332, 11 36.666666666666664}},
                     :Foo {:f-stat 3.730263157894737,
                           :means {2 18.0, 3 27.0, 5 45.0}}}}))))
