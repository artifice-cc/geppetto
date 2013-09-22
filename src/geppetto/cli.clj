(ns geppetto.cli
  (:use [clojure.tools.cli])
  (:use [geppetto.parameters :only [read-params extract-problem]])
  (:use [geppetto.random])
  (:use [geppetto.records :only [run-with-new-record]])
  (:use [geppetto.optimize :only [optimize]])
  (:use [geppetto.misc])
  (:use [propertea.core]))

(defn geppetto-cli [run-fn args]
  (let [[options _ banner]
        (cli args
             ["--action" "Action (run/optimize)" :default "run"]
             ["--params" "Parameters identifier (e.g. \"Prob/foo\")" :default ""]
             ["--nthreads" "Number of threads" :default 1 :parse-fn #(Integer. %)]
             ["--repetitions" "Number of repetitions" :default 10 :parse-fn #(Integer. %)]
             ["--seed" "Seed" :default 0 :parse-fn #(Integer. %)]
             ["--upload" "Upload?" :default true :parse-fn #(= "true" %)]
             ["--save-record" "Save in record directory?" :default true :parse-fn #(= "true" %)]
             ["--quiet" "Quiet mode (hide progress messages)?" :default false :parse-fn #(= "true" %)]
             ["--metric" "Optimize metric" :parse-fn keyword]
             ["--min-or-max" "Optimize to 'min' or 'max' of metric" :default :max :parse-fn keyword])
        props (read-properties "config.properties")]
    (setup-geppetto (:geppetto_dbhost props)
                    (:geppetto_dbport props)
                    (:geppetto_dbname props)
                    (:geppetto_dbuser props)
                    (:geppetto_dbpassword props)
                    (:quiet options))
    (alter-var-root (var rgen) (constantly (new-seed (:seed options))))
    (cond (and (or (= "run" (:action options))
                   (= "optimize" (:action options)))
               (= "" (:params options)))
          (println "--params identifier required.")

          (= "run" (:action options))
          (let [problem (extract-problem (:params options))]
            (run-with-new-record run-fn (:params options) (:datadir props) (:seed options)
                                 (:git props) (:recordsdir props) (:nthreads options)
                                 (:repetitions options) (:upload options) (:save-record options) false))

          (= "optimize" (:action options))
          (optimize run-fn (:params options) (:min-or-max options) (:metric options) 0.95 10 10 50
                    (:datadir props) (:seed options) (:git props) (:recordsdir props) (:nthreads options)
                    (:repetitions options) (:upload options) (:save-record options))

          :else
          (println "No action given."))))
