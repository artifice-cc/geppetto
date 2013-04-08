(ns geppetto.claim-test
  (:use [clojure.test])
  (:use [geppetto.claim])
  (:use [geppetto.stats])
  (:use [geppetto.test-fixtures])
  (:use [geppetto.test-utils])
  (:use [geppetto.parameters]))

(use-fixtures :each in-memory-db)

(deftest test-make-claim
  (let [claim (make-claim tracking-baseline-high-avgprec
                          (parameters "Tracking/baseline")
                          (verify {:control
                                   ((> (geppetto.stats/mean :_AvgPrec) 0.75)
                                    (> (geppetto.stats/mean :_AvgCoverage) 0.75))}))]
    (is (= 'tracking-baseline-high-avgprec (:name claim)))
    (is (= "Tracking/baseline" (:parameters claim)))
    (is (= '(> (geppetto.stats/mean :_AvgPrec) 0.75)
           (:code (first (:control (:verify claim))))))
    (is (= '(> (geppetto.stats/mean :_AvgCoverage) 0.75)
           (:code (second (:control (:verify claim))))))
    (is (= false (do (dosync (alter results
                                    (constantly [{:control [{:AvgPrec 1.0}]}
                                                 {:control [{:AvgPrec 0.0}]}])))
                     ((:result (first (:control (:verify claim))))))))
    (is (= true (do (dosync (alter results
                                   (constantly [{:control [{:AvgPrec 1.0}]}
                                                {:control [{:AvgPrec 2.0}]}])))
                    ((:result (first (:control (:verify claim))))))))))

(deftest test-evaluate-claim
  (let [claim (make-claim
               tracking-baseline-high-avgprec
               (parameters "Testing/test-1")
               (verify {:control
                        ((< (geppetto.stats/mean :_a) 1.0)
                         (> (geppetto.stats/mean :_b) 0.0)
                         (let [lm (geppetto.stats/linear-reg :_a :_b)]
                           (and (geppetto.test-utils/nearly= (first (:coefs lm)) 10.0 1.0)
                                (> (:r-square lm) 0.8))))}))
        run-fn (fn [comparative? params] (let [a (rand) b (* (+ 10 (rand)) a)] [{:a a :b b}]))
        eval-result (evaluate-claim run-fn claim "" "/usr/bin/git" "/tmp" 1)]
    (is (= true eval-result))))
