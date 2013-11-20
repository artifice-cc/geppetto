(ns geppetto.runs-test
  (:use [clojure.test])
  (:use [geppetto.runs])
  (:use [geppetto.records])
  (:use [geppetto.parameters])
  (:use [geppetto.test-fixtures]))

(use-fixtures :each setup-random-seed travis-mysql-db quiet-mode)

(def run-fn (fn [comparative? params]
              [{:result (* (:Foo params) (:Bar params))}]))

(deftest test-read-results
  (new-parameters {:problem "Testing"
                   :name "test-runs"
                   :control (pr-str {:Foo [2 3 5] :Bar [7 11]})
                   :description "testing params"})
  (let [runid (run-with-new-record
               run-fn "Testing/test-runs" "data" 0 "git" "test/records" 1 1 true true false)]
    (is (= (sort-by :simulation (get-results runid :control nil))
           (sort-by :simulation
                    [{:Seed 7309677, :params "{:simulation 0, :Seed 7309677, :Bar 7, :Foo 2}", :simulation 0, :result 14, :Foo 2, :Bar 7}
                     {:Seed 7309677, :params "{:simulation 1, :Seed 7309677, :Bar 7, :Foo 3}", :simulation 1, :result 21, :Foo 3, :Bar 7}
                     {:Seed 7309677, :params "{:simulation 3, :Seed 7309677, :Bar 11, :Foo 2}", :simulation 3, :result 22, :Foo 2, :Bar 11}
                     {:Seed 7309677, :params "{:simulation 4, :Seed 7309677, :Bar 11, :Foo 3}", :simulation 4, :result 33, :Foo 3, :Bar 11}
                     {:Seed 7309677, :params "{:simulation 2, :Seed 7309677, :Bar 7, :Foo 5}", :simulation 2, :result 35, :Foo 5, :Bar 7}
                     {:Seed 7309677, :params "{:simulation 5, :Seed 7309677, :Bar 11, :Foo 5}", :simulation 5, :result 55, :Foo 5, :Bar 11}])))
    (is (= (sort-by :result (get-results runid :control [:result :Foo]))
           (sort-by :result
                    [{:result 14 :Foo 2 :params "{:simulation 0, :Seed 7309677, :Bar 7, :Foo 2}"}
                     {:result 21 :Foo 3 :params "{:simulation 1, :Seed 7309677, :Bar 7, :Foo 3}"}
                     {:result 22 :Foo 2 :params "{:simulation 3, :Seed 7309677, :Bar 11, :Foo 2}"}
                     {:result 33 :Foo 3 :params "{:simulation 4, :Seed 7309677, :Bar 11, :Foo 3}"}
                     {:result 35 :Foo 5 :params "{:simulation 2, :Seed 7309677, :Bar 7, :Foo 5}"}
                     {:result 55 :Foo 5 :params "{:simulation 5, :Seed 7309677, :Bar 11, :Foo 5}"}])))))
