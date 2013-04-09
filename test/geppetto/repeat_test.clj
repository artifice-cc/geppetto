(ns geppetto.repeat-test
  (:use [clojure.test])
  (:use [geppetto.repeat]))

(deftest test-extract-single
  (let [rs {:control [{:a 1 :b 2} {:a 3 :b 4}]}]
    (is (= [{:a 1} {:a 3}]
           (extract-single (:control rs) :control {:control {:only [:a]}})))
    (is (= [{:b 2} {:b 4}]
           (extract-single (:control rs) :control {:control {:ignore [:a]}})))
    ;; only takes priority over ignore
    (is (= [{:a 1} {:a 3}]
           (extract-single (:control rs) :control {:control {:only [:a] :ignore [:a]}})))
    ;; key specified doesn't exist
    (is (= [{:a 1 :b 2} {:a 3 :b 4}]
           (extract-single (:control rs) :control {:control {:ignore [:z]}})))
    ;; nothing to ignore/only
    (is (= [{:a 1 :b 2} {:a 3 :b 4}]
           (extract-single (:control rs) :control {})))
    (is (= [{:a 1 :b 2} {:a 3 :b 4}]
           (extract-single (:control rs) :control {:control {}})))
    (is (= [{:a 1 :b 2} {:a 3 :b 4}]
           (extract-single (:control rs) :control {:comparison {:only [:a]}})))))

(deftest test-extract-relevant-results
  (let [results [{:control [{:a 1 :b 2} {:a 3 :b 4}]
                  :comparison [{:c 1 :d 2} {:c 3 :d 4}]
                  :comparative [{:e 1 :f 2} {:e 3 :f 4}]}
                 {:control [{:a 8 :b 9} {:a 10 :b 11}]
                  :comparison [{:c 8 :d 9} {:c 10 :d 11}]
                  :comparative [{:e 8 :f 9} {:e 10 :f 11}]}]]
    (is (= [{:control [{:a 1} {:a 3}]
             :comparison [{:c 1 :d 2} {:c 3 :d 4}]
             :comparative [{:e 1 :f 2} {:e 3 :f 4}]}
            {:control [{:a 8} {:a 10}]
             :comparison [{:c 8 :d 9} {:c 10 :d 11}]
             :comparative [{:e 8 :f 9} {:e 10 :f 11}]}]
           (extract-relevant-results results {:control {:only [:a]}})))
    (is (= [{:control [{:b 2} {:b 4}]
             :comparison [{:c 1} {:c 3}]
             :comparative [{:e 1 :f 2} {:e 3 :f 4}]}
            {:control [{:b 9} {:b 11}]
             :comparison [{:c 8} {:c 10}]
             :comparative [{:e 8 :f 9} {:e 10 :f 11}]}]
           (extract-relevant-results results {:control {:ignore [:a]}
                                              :comparison {:only [:c]}})))
    ;; only-ignore == {}, results should not be changed
    (is (= [{:control [{:a 1 :b 2} {:a 3 :b 4}]
             :comparison [{:c 1 :d 2} {:c 3 :d 4}]
             :comparative [{:e 1 :f 2} {:e 3 :f 4}]}
            {:control [{:a 8 :b 9} {:a 10 :b 11}]
             :comparison [{:c 8 :d 9} {:c 10 :d 11}]
             :comparative [{:e 8 :f 9} {:e 10 :f 11}]}]
           (extract-relevant-results results {})))))
