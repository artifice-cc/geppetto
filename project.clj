(defproject cc.artifice/geppetto "2.5.0-SNAPSHOT"
  :description "Backend support for experimental work."
  :url "http://geppetto.artifice.cc"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
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
                 [com.h2database/h2 "1.3.171"]]
  :profiles {:dev {:dependencies
                   [[codox "0.6.6"]]}}
  :codox {:src-dir-uri "http://github.com/artifice-cc/geppetto/blob/master/"
          :src-linenum-anchor-prefix "L"})
