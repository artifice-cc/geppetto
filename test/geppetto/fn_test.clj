(ns geppetto.fn-test
  (:use [clojure.test])
  (:require [plumbing.graph :as graph])
  (:require [plumbing.core :as p])
  (:require [clojure.core.cache :as cache])
  (:require [loom.graph :as loom])
  (:require [loom.io :as loom.io])
  (:use [geppetto.fn])
  (:use [geppetto.fnviz]))

(deftest test-fn-params
  (let [g1 (paramfnk [x y] [a [1 2 3] b (range 4 7)] (* x y a b))
        g2 {:result (paramfnk [x y] [a [1 2 3] b [4 5 6]] (* x y a b))}
        g3 {:result g1}
        g4 (paramfnk [x y] [] (* x y))
        g5 {:x (paramfnk [y] [] 0) ;; cycle, bad graph
            :y (paramfnk [x] [] 0)}
        f1 (compile-graph graph/eager-compile g1)
        f2 (compile-graph graph/eager-compile g2)
        f3 (compile-graph graph/eager-compile g3)
        f4 (compile-graph graph/eager-compile g4)]
    (is (= (* 1 2 3 4) (f1 {:x 1 :y 2 :params {:a 3 :b 4}})))
    (is (= (* 1 2 3 4) (:result (f2 {:x 1 :y 2 :params {:a 3 :b 4}}))))
    (is (= (* 1 2 3 4) (:result (f3 {:x 1 :y 2 :params {:a 3 :b 4}}))))
    (is (= [:a :b] (fn-params g1)))
    (is (= [1 2 3] (fn-param-range g1 :a)))
    (is (= [4 5 6] (fn-param-range g1 :b)))
    (is (= [:a :b] (fn-params f1)))
    (is (= [1 2 3] (fn-param-range f1 :a)))
    (is (= [4 5 6] (fn-param-range f1 :b)))
    (is (vector? (fn-param-range f1 :b)))
    (is (= [:a :b] (fn-params f2)))
    (is (= [1 2 3] (fn-param-range f2 :a)))
    (is (= [4 5 6] (fn-param-range f2 :b)))
    (is (= [:a :b] (fn-params f3)))
    (is (= [1 2 3] (fn-param-range f3 :a)))
    (is (= [4 5 6] (fn-param-range f3 :b)))
    (is (= '[x y] (:bindings (meta g1))))
    (is (= 12 (f4 {:x 3 :y 4 :params {}})))
    (is (thrown? java.lang.IllegalArgumentException (compile-graph graph/eager-compile g5)))))

(deftest test-all-fn-params
  (let [g1 (paramfnk [x y] [a [1 2] b [3 4]] (* x y a b))
        g2 {:x (paramfnk [y] [a [1 2]] (+ y a))
            :z (paramfnk [x y] [b [3 4]] (+ x y b))}
        f1 (compile-graph graph/eager-compile g1)
        f2 (compile-graph graph/eager-compile g2)]
    (is (= {:a [1 2] :b [3 4]} (all-fn-params g1)))
    (is (= [{:a 1 :b 3} {:a 1 :b 4}
            {:a 2 :b 3} {:a 2 :b 4}]
           (all-fn-params-combinations g1)))
    (is (= {:a [1 2] :b [3 4]} (all-fn-params f1)))
    (is (= [{:a 1 :b 3} {:a 1 :b 4}
            {:a 2 :b 3} {:a 2 :b 4}]
           (all-fn-params-combinations f1)))
    (is (= {:a [1 2] :b [3 4]} (all-fn-params g2)))
    (is (= [{:a 1 :b 3} {:a 1 :b 4}
            {:a 2 :b 3} {:a 2 :b 4}]
           (all-fn-params-combinations g2)))
    (is (= {:a [1 2] :b [3 4]} (all-fn-params f2)))
    (is (= [{:a 1 :b 3} {:a 1 :b 4}
            {:a 2 :b 3} {:a 2 :b 4}]
           (all-fn-params-combinations f2)))))

(deftest test-with-params
  (let [g1 (paramfnk [x y] [a [1 2] c [3 4]] (* x y a c))
        g2 {:x (paramfnk [y] [a [1 2]] (+ y a))
            :z (paramfnk [x y] [b [3 4]] (+ x y b))}
        f1 (compile-graph graph/eager-compile g1)
        f2 (compile-graph graph/eager-compile g2)
        f (fn-with-params [x y] (let [foo 1]
                                  (+ (f1 {:x x :y foo :params params})
                                     (:z (f2 {:x x :y y :params params})))))]
    (is (= (+ (* 5 1 2 8) (+ (+ 2 2) 2 3))
           (f {:x 5 :y 2 :params {:a 2 :b 3 :c 8}})))
    (is (= {:a [1 2], :b [3 4] :c [3 4]}
           (all-fn-params f)))
    (is (= '[x y] (:bindings (meta f))))))

(deftest test-paramfnk-when
  (let [g1 (paramfnk [x y] [a [1 2] c [3 4]]
                     :when {:cond (> x 5) :else 0}
                     (* x y a c))
        did-compute? (atom {})
        g2 {:x (paramfnk [y] [a [1 2]]
                         :when {:cond (> y 5) :else 1}
                         (swap! did-compute? assoc :x true) (+ y a))
            :z (paramfnk [x y] [b [3 4]]
                         :when {:cond (< x 2) :else -1}
                         (swap! did-compute? assoc :z true) (+ x y b))}
        f1 (compile-graph graph/eager-compile g1)
        f2 (compile-graph graph/eager-compile g2)]
    (is (= 24 (f1 {:x 6 :y 1 :params {:a 1 :c 4}})))
    (is (= 0 (f1 {:x 2 :y 1 :params {:a 1 :c 4}})))
    (reset! did-compute? {})
    (is (= 1 (:x (f2 {:x 6 :y 1 :params {:a 1 :b 5 :c 4}}))))
    (is (not (:x @did-compute?)))
    (reset! did-compute? {})
    (is (= 7 (:z (f2 {:x 6 :y 1 :params {:a 1 :b 5 :c 4}}))))
    (is (not (:x @did-compute?)))
    (is (:z @did-compute?))))

(deftest test-paramfnk-hashes-1
  (let [did-compute? (atom {})
        g1 {:x (paramfnk-hashed [a] [] (swap! did-compute? assoc :x true) (+ a 2))
            :y (paramfnk-hashed [x] [] (swap! did-compute? assoc :y true) (+ x 3))
            :z (paramfnk-hashed [x y] [] (swap! did-compute? assoc :z true) (+ x y 4))}
        f1 (compile-graph graph/eager-compile g1)]
    (is (= 2 (:x (f1 {:a 0 :params {} :hashes {:x (hash 2) :y (hash 5) :z (hash 11)}}))))
    (is (get @did-compute? :x))
    (is (not (get @did-compute? :y)))
    (is (not (get @did-compute? :z)))))

(deftest test-paramfnk-hashes-2
  (let [did-compute? (atom {})
        g1 {:x (paramfnk-hashed [a] [] (swap! did-compute? assoc :x true) (+ a 2))
            :y (paramfnk-hashed [x] [] (swap! did-compute? assoc :y true) (+ x 3))
            :z (paramfnk-hashed [y] [] (swap! did-compute? assoc :z true) (+ y 4))}
        f1 (compile-graph graph/eager-compile g1)
        result (f1 {:a 0 :params {} :hashes {:y (hash 5) :z (hash 11)}})]
    ;; x is computed, but its output doesn't match the hash (no hash provided)
    ;; so y is recomputed, and its output does match;
    ;; thus, z is not recomputed
    (is (= 2 (:x result)))
    (is (= 5 (:y result)))
    (is (and (delay? (:z result)) (not (realized? (:z result)))))
    (is (get @did-compute? :x))
    (is (get @did-compute? :y))
    (is (not (get @did-compute? :z)))))

(deftest test-paramfnk-hashes-3
  (let [did-compute? (atom {})
        g1 {:x (paramfnk-hashed [a] [] (swap! did-compute? assoc :x true) (+ a 2))
            :y (paramfnk-hashed [x] [] (swap! did-compute? assoc :y true) (+ x 3))
            :z (paramfnk-hashed [x y] [] (swap! did-compute? assoc :z true) (+ x y 4))}
        f1 (compile-graph graph/eager-compile g1)
        result (f1 {:a 0 :params {} :hashes {:y (hash 5) :z (hash 11)}})]
    ;; x is computed, but its output doesn't match the hash (no hash provided)
    ;; so y is recomputed, and its output does match;
    ;; thus, z is recomputed since it depends on x
    (is (= 2 (:x result)))
    (is (= 5 (:y result)))
    (is (= 11 (:z result)))
    (is (get @did-compute? :x))
    (is (get @did-compute? :y))
    (is (get @did-compute? :z))))

(deftest test-paramfnk-hashes-4
  (let [did-compute? (atom {})
        g1 {:x (paramfnk-hashed [a] [] (swap! did-compute? assoc :x true) (+ a 2))
            :y (paramfnk-hashed [x] [] (swap! did-compute? assoc :y true) (+ x 3))
            :z (paramfnk-hashed [x y] [] (swap! did-compute? assoc :z true) (+ x y 4))}
        f1 (compile-graph graph/eager-compile g1)
        result (f1 {:a 0 :params {} :hashes {}})]
    ;; everybody is recomputed because there are no hashes
    (is (= 2 (:x result)))
    (is (= 5 (:y result)))
    (is (= 11 (:z result)))
    (is (get @did-compute? :x))
    (is (get @did-compute? :y))
    (is (get @did-compute? :z))))

(deftest test-fnkc
  (let [f1 (fnkc f1 [x y] (+ x y))
        c (atom (cache/lru-cache-factory {}))]
    (is (= (f1 {:x 1 :y 2 :cache c}) 3))
    (is (= @c {{:fn-name :f1 :args {:x 1 :y 2}} 3}))))

(deftest test-fnkc-2
  (let [g1 {:foo (fnkc foo [x y] (+ x y))
            :baz (fnkc baz [x y] (/ x y))
            :bar (fnkc bar [foo z] (* foo z))}
        f1 (compile-graph graph/eager-compile g1)
        c (atom (cache/lru-cache-factory {}))]
    (let [result (f1 {:x 1 :y 2 :z 3 :cache c})]
      (is (= (* (+ 1 2) 3) (:bar result)))
      (is (= (/ 1 2) (:baz result))))
    (is (= @c {{:fn-name :foo :args {:y 2 :x 1}} 3
               {:fn-name :baz :args {:y 2 :x 1}} (/ 1 2)
               {:fn-name :bar :args {:foo 3 :z 3}} 9}))))

(deftest test-paramfnkc
  (let [f1 (paramfnkc f1 [x y] [foo [11 12] bar [13 14]] (+ x y foo bar))
        c (atom (cache/lru-cache-factory {}))]
    (is (= (f1 {:x 1 :y 2 :cache c :params {:foo 11 :bar 13}})
           (+ 1 2 11 13)))
    (is (= @c {{:fn-name :f1 :args {:x 1 :y 2 :params {:foo 11 :bar 13}}} (+ 1 2 11 13)}))))

(deftest test-deps
  (let [g {:fulltext (p/fnk [f] "fulltext")
           :title (p/fnk [f] "title")
           :summary (p/fnk [fulltext title] "summary")
           :concept-tags (p/fnk [fulltext title] "concept-tags")}]
    (is (= {:need #{:f} :path [:fulltext]} (generate-fn-path g #{:fulltext} #{})))
    (is (= {:need #{:f} :path [:fulltext]} (generate-fn-path g #{:fulltext} #{:f})))
    (is (= {:need #{:f} :path [:title]} (generate-fn-path g #{:title} #{:f :fulltext :summary :concept-tags})))
    (is (= {:need #{:f} :path [:fulltext :title :concept-tags :summary]} (generate-fn-path g #{:fulltext :concept-tags :summary} #{:f})))
    (is (= {:need #{:fulltext :f} :path [:title :summary]} (generate-fn-path g #{:summary} #{:fulltext})))
    (is (= {:need #{:fulltext :f} :path [:title :summary]} (generate-fn-path g #{:summary} #{:f :fulltext})))
    (is (= {:need #{:fulltext :f} :path [:title :concept-tags]} (generate-fn-path g #{:concept-tags} #{:f :fulltext :summary})))
    (is (= {:need #{:fulltext :title} :path [:concept-tags]} (generate-fn-path g #{:concept-tags} #{:fulltext :title})))
    (is (= {:need #{:fulltext :title} :path [:concept-tags]} (generate-fn-path g #{:concept-tags} #{:fulltext :title :summary})))
    (is (= {:need #{:fulltext :title} :path [:concept-tags]} (generate-fn-path g #{:concept-tags :title} #{:fulltext :title :summary})))
    (is (= {:need #{:f} :path [:fulltext :title :summary]} (generate-fn-path g #{:summary} #{})))
    (is (= {:need #{:f} :path [:fulltext :title :concept-tags :summary]} (generate-fn-path g #{:summary :concept-tags} #{})))
    (is (= {:need #{:f} :path [:fulltext :title :concept-tags]} (generate-fn-path g #{:summary :concept-tags} #{:summary})))
    (is (= {:need #{} :path []} (generate-fn-path g #{:summary} #{:summary})))
    (is (= {:need #{} :path []} (generate-fn-path g #{:summary} #{:title :concept-tags :summary})))
    (is (= {:need #{} :path []} (generate-fn-path g #{:summary :title} #{:title :concept-tags :summary})))
    (is (= {:need #{} :path []} (generate-fn-path g #{:summary :title :concept-tags} #{:title :concept-tags :summary})))
    ;; cycle, bad graph
    (is (thrown? java.lang.IllegalArgumentException
                 (compile-graph graph/eager-compile {:fulltext (p/fnk [summary] nil)
                                                     :summary (p/fnk [fulltext] nil)})))))

