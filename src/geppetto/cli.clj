(ns geppetto.cli
  (:require [clojure.string :as str])
  (:use [clojure.tools.cli])
  (:use [geppetto.parameters :only [read-params extract-problem]])
  (:use [geppetto.random])
  (:use [geppetto.records :only [run-with-new-record]])
  (:use [geppetto.optimize :only [optimize]])
  (:use [geppetto.runs :only [get-run find-abandoned-runs]])
  (:use [geppetto.repeat :only [verify-identical-repeat-run]])
  (:use [clojure.pprint :only [pprint]])
  (:use [geppetto.misc])
  (:use [propertea.core])
  (:use [taoensso.timbre]))

(defn geppetto-cli [run-fn args]
  (let [[options _ banner]
        (cli args
             ["--action" "Action (run/optimize/verify-identical/list-abandoned)" :default "run"]
             ["--params" "Parameters identifier (e.g. \"Prob/foo\")" :default ""]
             ["--nthreads" "Number of threads" :default 1 :parse-fn #(Integer. %)]
             ["--repetitions" "Number of repetitions" :default 10 :parse-fn #(Integer. %)]
             ["--seed" "Seed" :default 0 :parse-fn #(Integer. %)]
             ["--runid" "Run ID for repeating" :default "" :parse-fn #(Integer. %)]
             ["--upload" "Upload?" :default true :parse-fn #(= "true" %)]
             ["--save-record" "Save in record directory?" :default true :parse-fn #(= "true" %)]
             ["--quiet" "Quiet mode (hide progress messages)?" :default false :parse-fn #(= "true" %)]
             ["--opt-metric" "Optimize metric" :parse-fn keyword]
             ["--opt-min-or-max" "Optimize to 'min' or 'max' of metric" :default :max :parse-fn keyword]
             ["--opt-alpha" "Optimize alpha (double)" :default 0.95 :parse-fn #(Double. %)]
             ["--opt-init-temp" "Optimize initial temperature (double)" :default 1 :parse-fn #(Double. %)]
             ["--opt-temp-sched" "Optimize temperature schedule (int)" :default 100 :parse-fn #(Integer. %)]
             ["--opt-stop-cond1" "Optimize stopping condition 1 (double)" :default 0.02 :parse-fn #(Double. %)]
             ["--opt-stop-cond2" "Optimize stopping condition 2 (int)" :default 5 :parse-fn #(Integer. %)])
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
          (fatal "--params identifier required.")

          (= "run" (:action options))
          (let [problem (extract-problem (:params options))]
            (run-with-new-record (partial run-fn problem) (:params options) (:datadir props) (:seed options)
                                 (:git props) (:recordsdir props) (:nthreads options)
                                 (:repetitions options) (:upload options) (:save-record options) false))

          (= "optimize" (:action options))
          (let [problem (extract-problem (:params options))]
            (optimize (partial run-fn problem)
                      (:params options) (:opt-min-or-max options) (:opt-metric options)
                      (:opt-alpha options) (:opt-init-temp options) (:opt-temp-sched options)
                      (:opt-stop-cond1 options) (:opt-stop-cond2 options)
                      (:datadir props) (:seed options) (:git props) (:recordsdir props) (:nthreads options)
                      (:repetitions options) (:upload options) (:save-record options)))

          (= (:action options) "verify-identical")
          (let [problem (:problem (get-run (:runid options)))
                only-ignore {:control {:ignore [:Milliseconds]}
                             :comparison {:ignore [:Milliseconds]}
                             :comparative {:ignore [:Milliseconds]}}]
            (pprint (verify-identical-repeat-run
                     (:runid options) only-ignore (partial run-fn problem)
                     (:datadir props) (:git props) (:nthreads options))))

          (= (:action options) "list-abandoned")
          (println (str/join "\n" (sort (find-abandoned-runs (:recordsdir props)))))

          :else
          (fatal "No action given."))))
