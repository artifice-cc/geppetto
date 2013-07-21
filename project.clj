(defproject cc.artifice/geppetto "2.4.0-SNAPSHOT"
  :description "Backend support for experimental work."
  :url "http://artifice.cc/geppetto"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/java.jdbc "0.2.3"]
                 [korma "0.3.0-RC2"]
                 [mysql/mysql-connector-java "5.1.6"]
                 [org.apache.commons/commons-math3 "3.0"]
                 [clojure-csv/clojure-csv "2.0.0-alpha1"]
                 [incanter "1.5.0-SNAPSHOT"]
                 [cc.artifice/resque-clojure "0.3.0-SNAPSHOT"]]
  :profiles {:dev {:dependencies
                   [[com.h2database/h2 "1.3.171"]]}})
