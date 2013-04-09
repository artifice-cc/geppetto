(ns geppetto.repeat-test
  (:use [clojure.test])
  (:use [geppetto.repeat]))

(deftest test-extract-single
  (let [rs {:control {:a 1 :b 2}}]
    (is (= {:a 1}
           (extract-single (:control rs) :control {:control {:only [:a]}})))
    (is (= {:b 2}
           (extract-single (:control rs) :control {:control {:ignore [:a]}})))
    ;; only takes priority over ignore
    (is (= {:a 1}
           (extract-single (:control rs) :control {:control {:only [:a] :ignore [:a]}})))
    ;; key specified doesn't exist
    (is (= {:a 1 :b 2}
           (extract-single (:control rs) :control {:control {:ignore [:z]}})))
    ;; nothing to ignore/only
    (is (= {:a 1 :b 2}
           (extract-single (:control rs) :control {})))
    (is (= {:a 1 :b 2}
           (extract-single (:control rs) :control {:control {}})))
    (is (= {:a 1 :b 2}
           (extract-single (:control rs) :control {:comparison {:only [:a]}})))))

(deftest test-extract-relevant-results
  (let [results [{:control {:a 1 :b 2 :params "foo"}
                  :comparison {:c 1 :d 2 :params "foo"}
                  :comparative {:e 1 :f 2 :params "foo"}}
                 {:control {:a 8 :b 9 :params "foo"}
                  :comparison {:c 8 :d 9 :params "foo"}
                  :comparative {:e 8 :f 9 :params "foo"}}]]
    (is (= [{:control {:a 1}
             :comparison {:c 1 :d 2}
             :comparative {:e 1 :f 2}}
            {:control {:a 8}
             :comparison {:c 8 :d 9}
             :comparative {:e 8 :f 9}}]
           (extract-relevant-results results {:control {:only [:a]}})))
    (is (= [{:control {:b 2}
             :comparison {:c 1}
             :comparative {:e 1 :f 2}}
            {:control {:b 9}
             :comparison {:c 8}
             :comparative {:e 8 :f 9}}]
           (extract-relevant-results results {:control {:ignore [:a]}
                                              :comparison {:only [:c]}})))
    ;; only-ignore == {}, results should not be changed
    (is (= [{:control {:a 1 :b 2}
             :comparison {:c 1 :d 2}
             :comparative {:e 1 :f 2}}
            {:control {:a 8 :b 9}
             :comparison {:c 8 :d 9}
             :comparative {:e 8 :f 9}}]
           (extract-relevant-results results {})))))
