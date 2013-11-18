(defproject cc.artifice/geppetto "3.0.0-SNAPSHOT"
  :description "Backend support for experimental work."
  :url "http://geppetto.artifice.cc"
  :license {:name "MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/tools.cli "0.2.2"]
                 [propertea "1.2.3"]
                 [korma "0.3.0-RC5"]
                 [mysql/mysql-connector-java "5.1.6"]
                 [org.apache.commons/commons-math3 "3.0"]
                 [clj-time "0.6.0"]
                 [clojure-csv/clojure-csv "2.0.0-alpha1"]
                 [incanter "1.5.4"]
                 [cc.artifice/resque-clojure "0.3.0-SNAPSHOT"]
                 [cc.artifice/timbre "2.6.3"]
                 [prismatic/plumbing "0.1.1"]
                 [prismatic/schema "0.1.6"]]
  :profiles {:dev {:dependencies
                   [[codox "0.6.6"]]}}
  :codox {:src-dir-uri "http://github.com/artifice-cc/geppetto/blob/master/"
          :src-linenum-anchor-prefix "L"})
