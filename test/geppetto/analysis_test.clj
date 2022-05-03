(ns geppetto.analysis-test
  (:use [clojure.test])
  (:use [clojure.data])
  (:use [geppetto.fn])
  (:use [geppetto.analysis])
  (:use [geppetto.runs])
  (:use [geppetto.records])
  (:use [geppetto.parameters])
  (:use [geppetto.test-fixtures])
  (:require [plumbing.graph :as graph]))

(use-fixtures :each setup-random-seed in-memory-db quiet-mode)

(def g (graph/graph {:sum (paramfnk [x] [a [1 2]] (+ a x))
                     :prod (paramfnk [sum y] [b [3 4]] (* y sum b))}))

(def g2 (graph/graph {:foo (paramfnk [] [a [1 2 3 4] b [1 2 3 4]] 55.0)}))

(deftest test-try-all
  (is (= {{:a 2 :b 5} (float (* 100 (+ 2 11) 5))}
         (try-all g {:x 11 :y 100} {:a 2 :b 5} :prod 1)))
  (is (= {{:a 2 :b 3} (float (* 100 (+ 2 11) 3))
          {:a 2 :b 4} (float (* 100 (+ 2 11) 4))}
         (try-all g {:x 11 :y 100} {:a 2} :prod 1)))
  (is (= {{:a 1 :b 3} (float (* 100 (+ 1 11) 3))
          {:a 1 :b 4} (float (* 100 (+ 1 11) 4))
          {:a 2 :b 3} (float (* 100 (+ 2 11) 3))
          {:a 2 :b 4} (float (* 100 (+ 2 11) 4))}
         (try-all g {:x 11 :y 100} {} :prod 10))))

(comment
  (deftest test-calc-effect
    (let [f (compile-graph graph/eager-compile g)
          f2 (compile-graph graph/eager-compile g2)]
      (is (= (calc-effect (for [params (all-fn-params-combinations f)]
                            (assoc (f {:x 0 :y 10 :params params})
                              :params (pr-str params)))
                          :prod)
             {:a {:sample-size 4, :overall-mean 52.5, :f-stat 9.8,
                  :p-value 0.08867762313423289, :between-group-var 1225.0,
                  :df1 1, :within-group-var 125.0, :means {1 35.0, 2 70.0}, :df2 2,
                  :val-stats #{{:val 1, :n 2, :m 35.0, :results [30 40] :ss 50.0}
                               {:val 2, :n 2, :m 70.0, :results [60 80] :ss 200.0}}},
              :b {:sample-size 4, :overall-mean 52.5, :f-stat 0.36,
                  :p-value 0.6094332670575284, :between-group-var 225.0, :df1 1,
                  :within-group-var 625.0, :means {3 45.0, 4 60.0}, :df2 2,
                  :val-stats #{{:val 3, :n 2, :m 45.0, :results [30 60] :ss 450.0}
                               {:val 4, :n 2, :m 60.0, :results [40 80] :ss 800.0}}}}))
      (is (= (calc-effect (for [params (all-fn-params-combinations f2)]
                            (assoc (f2 {:params params})
                              :params (pr-str params)))
                          :foo)
             {:a {:sample-size 16, :overall-mean 55.0,
                  :f-stat nil, :p-value nil, :between-group-var 0.0,
                  :df1 3, :within-group-var 0.0, :means {1 55.0, 3 55.0, 2 55.0, 4 55.0}, :df2 12,
                  :val-stats #{{:val 1, :n 4, :m 55.0, :results [55.0 55.0 55.0 55.0], :ss 0.0}
                               {:val 3, :n 4, :m 55.0, :results [55.0 55.0 55.0 55.0], :ss 0.0}
                               {:val 2, :n 4, :m 55.0, :results [55.0 55.0 55.0 55.0], :ss 0.0}
                               {:val 4, :n 4, :m 55.0, :results [55.0 55.0 55.0 55.0], :ss 0.0}}},
              :b {:sample-size 16, :overall-mean 55.0, :f-stat nil, :p-value nil,
                  :between-group-var 0.0, :df1 3, :within-group-var 0.0,
                  :means {4 55.0, 1 55.0, 2 55.0, 3 55.0}, :df2 12,
                  :val-stats #{{:val 4, :n 4, :m 55.0, :results [55.0 55.0 55.0 55.0], :ss 0.0}
                               {:val 1, :n 4, :m 55.0, :results [55.0 55.0 55.0 55.0], :ss 0.0}
                               {:val 2, :n 4, :m 55.0, :results [55.0 55.0 55.0 55.0], :ss 0.0}
                               {:val 3, :n 4, :m 55.0, :results [55.0 55.0 55.0 55.0], :ss 0.0}}}})))))

(def run-fn (fn [comparative? params]
              [{:early-result (+ (:Foo params) (:Bar params))
                :result (* (:Foo params) (:Bar params))
                :some-string "baz"}]))

(comment
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
             {:Bar {:sample-size 6, :overall-mean 30.0, :f-stat 1.3445378151260503,
                    :p-value 0.3107357548883588, :between-group-var 266.66666666666663,
                    :df1 1, :within-group-var 198.33333333333331,
                    :means {7 23.333333333333332, 11 36.666666666666664}, :df2 4,
                    :val-stats #{{:val 7, :n 3, :m 23.333333333333332,
                                  :results [14 35 21], :ss 228.66666666666669}
                                 {:val 11, :n 3, :m 36.666666666666664,
                                  :results [55 22 33], :ss 564.6666666666666}}},
              :Foo {:sample-size 6, :overall-mean 30.0, :f-stat 3.730263157894737,
                    :p-value 0.1535859815245415, :between-group-var 378.0, :df1 2,
                    :within-group-var 101.33333333333333, :means {5 45.0, 2 18.0, 3 27.0},
                    :df2 3, :val-stats #{{:val 5, :n 2, :m 45.0, :results [55 35], :ss 200.0}
                                         {:val 2, :n 2, :m 18.0, :results [14 22], :ss 32.0}
                                         {:val 3, :n 2, :m 27.0, :results [33 21], :ss 72.0}}}}))
      (is (= (calc-effect results)
             {:early-result {:Bar {:sample-size 6, :overall-mean 12.333333333333334,
                                   :f-stat 10.285714285714285, :p-value 0.03267792333680297,
                                   :between-group-var 24.0, :df1 1,
                                   :within-group-var 2.3333333333333335,
                                   :means {11 14.333333333333334, 7 10.333333333333334}, :df2 4,
                                   :val-stats #{{:val 11, :n 3, :m 14.333333333333334,
                                                 :results [13 14 16], :ss 4.666666666666667}
                                                {:val 7, :n 3, :m 10.333333333333334,
                                                 :results [9 10 12], :ss 4.666666666666667}}},
                             :Foo {:sample-size 6, :overall-mean 12.333333333333334,
                                   :f-stat 0.5833333333333334, :p-value 0.610940258945177,
                                   :between-group-var 4.666666666666667, :df1 2,
                                   :within-group-var 8.0, :means {3 12.0, 2 11.0, 5 14.0}, :df2 3,
                                   :val-stats #{{:val 3, :n 2, :m 12.0,
                                                 :results [10 14], :ss 8.0}
                                                {:val 2, :n 2, :m 11.0,
                                                 :results [9 13], :ss 8.0}
                                                {:val 5, :n 2, :m 14.0,
                                                 :results [12 16], :ss 8.0}}}},
              :result {:Bar {:sample-size 6, :overall-mean 30.0, :f-stat 1.3445378151260503,
                             :p-value 0.3107357548883588, :between-group-var 266.66666666666663,
                             :df1 1, :within-group-var 198.33333333333331,
                             :means {7 23.333333333333332, 11 36.666666666666664}, :df2 4,
                             :val-stats #{{:val 7, :n 3, :m 23.333333333333332,
                                           :results [14 21 35], :ss 228.66666666666669}
                                          {:val 11, :n 3, :m 36.666666666666664,
                                           :results [22 33 55], :ss 564.6666666666666}}},
                       :Foo {:sample-size 6, :overall-mean 30.0, :f-stat 3.730263157894737,
                             :p-value 0.1535859815245415, :between-group-var 378.0, :df1 2,
                             :within-group-var 101.33333333333333,
                             :means {5 45.0, 2 18.0, 3 27.0}, :df2 3,
                             :val-stats #{{:val 5, :n 2, :m 45.0, :results [35 55], :ss 200.0}
                                          {:val 2, :n 2, :m 18.0, :results [14 22], :ss 32.0}
                                          {:val 3, :n 2, :m 27.0, :results [21 33], :ss 72.0}}}}}))
      (is (= (calc-effect results-all)
             {:early-result
              {:Bar {:sample-size 6, :overall-mean 12.333333333333334,
                     :f-stat 10.285714285714285, :p-value 0.03267792333680297,
                     :between-group-var 24.0, :df1 1, :within-group-var 2.3333333333333335,
                     :means {11 14.333333333333334, 7 10.333333333333334}, :df2 4,
                     :val-stats #{{:val 11, :n 3, :m 14.333333333333334,
                                   :results [13 14 16] :ss 4.666666666666667}
                                  {:val 7, :n 3, :m 10.333333333333334,
                                   :results [9 10 12] :ss 4.666666666666667}}},
               :Foo {:sample-size 6, :overall-mean 12.333333333333334,
                     :f-stat 0.5833333333333334, :p-value 0.610940258945177,
                     :between-group-var 4.666666666666667, :df1 2, :within-group-var 8.0,
                     :means {3 12.0, 2 11.0, 5 14.0}, :df2 3,
                     :val-stats #{{:val 3, :n 2, :m 12.0, :results [10 14] :ss 8.0}
                                  {:val 2, :n 2, :m 11.0, :results [9 13] :ss 8.0}
                                  {:val 5, :n 2, :m 14.0, :results [12 16] :ss 8.0}}}},
              :result {:Bar {:sample-size 6, :overall-mean 30.0, :f-stat 1.3445378151260503,
                             :p-value 0.3107357548883588, :between-group-var 266.66666666666663,
                             :df1 1, :within-group-var 198.33333333333331,
                             :means {7 23.333333333333332, 11 36.666666666666664}, :df2 4,
                             :val-stats #{{:val 7, :n 3, :m 23.333333333333332,
                                           :results [14 21 35] :ss 228.66666666666669}
                                          {:val 11, :n 3, :m 36.666666666666664,
                                           :results [22 33 55] :ss 564.6666666666666}}},
                       :Foo {:sample-size 6, :overall-mean 30.0, :f-stat 3.730263157894737,
                             :p-value 0.1535859815245415, :between-group-var 378.0, :df1 2,
                             :within-group-var 101.33333333333333,
                             :means {5 45.0, 2 18.0, 3 27.0}, :df2 3,
                             :val-stats #{{:val 5, :n 2, :m 45.0, :results [35 55] :ss 200.0}
                                          {:val 2, :n 2, :m 18.0, :results [14 22] :ss 32.0}
                                          {:val 3, :n 2, :m 27.0, :results [21 33] :ss 72.0}}}}})))))
