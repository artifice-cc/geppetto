(ns geppetto.claim-test
  (:use [clojure.test])
  (:use [geppetto.claim])
  (:use [geppetto.stats])
  (:use [geppetto.test-fixtures])
  (:use [geppetto.parameters]))

(use-fixtures :each in-memory-db)

(deftest test-make-claim
  (let [claim (make-claim tracking-baseline-high-avgprec
                          (parameters "Tracking/baseline")
                          (verify {:control ((> (mean :AvgPrec) 0.75)
                                             (> (mean :AvgCoverage) 0.75))}))]
    (is (= 'tracking-baseline-high-avgprec (:name claim)))
    (is (= "Tracking/baseline" (:parameters claim)))
    (is (= '(> (mean :AvgPrec) 0.75) (:code (first (:control (:verify claim))))))
    (is (= '(> (mean :AvgCoverage) 0.75) (:code (second (:control (:verify claim))))))
    (is (= false ((:result (first (:control (:verify claim))))
                  [{:control [{:AvgPrec 1.0}]}
                   {:control [{:AvgPrec 0.0}]}])))
    (is (= true ((:result (first (:control (:verify claim))))
                 [{:control [{:AvgPrec 1.0}]}
                  {:control [{:AvgPrec 2.0}]}])))))

(deftest test-evaluate-claim
  (let [claim (make-claim tracking-baseline-high-avgprec
                          (parameters "Testing/test-1")
                          (verify {:control
                                   ((> (mean :a) 0.75)
                                    (> (mean :b) 0.75))}))
        run-fn (fn [comparative? params] [{:a 1.0 :b 1.0}])
        [problem-name ps] (read-params "Testing/test-1")
        eval-result (evaluate-claim run-fn claim ps "" "/usr/bin/git" "/tmp" 1)]
    (is (= true eval-result))))
