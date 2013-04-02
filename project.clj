(defproject cc.artifice/geppetto "2.1.0"
  :description "Backend support for experimental work."
  :url "http://artifice.cc/geppetto"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [org.clojure/java.jdbc "0.2.3"]
                 [korma "0.3.0-RC2"]
                 [mysql/mysql-connector-java "5.1.6"]
                 [org.apache.commons/commons-math3 "3.0"]
                 [clojure-csv/clojure-csv "2.0.0-alpha1"]
                 [incanter "1.4.1"]
                 [resque-clojure "0.2.2"]])
