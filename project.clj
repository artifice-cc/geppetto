(defproject cc.artifice/geppetto "3.3.0-SNAPSHOT"
  :description "Backend support for experimental work."
  :license {:name "MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/tools.cli "0.4.2"]
                 [org.clojure/tools.macro "0.1.5"]
                 [org.clojure/core.cache "1.0.207"]
                 [myguidingstar/clansi "1.3.0"]
                 [cc.artifice/propertea "1.4.1"]
                 [org.clojars.ejschoen/korma "0.4.5"]
                 [mysql/mysql-connector-java "8.0.16" :exclusions [com.google.protobuf/protobuf-java]]
                 [com.h2database/h2 "2.1.210"]
                 [org.apache.commons/commons-math3 "3.6.1"]
                 [clj-time "0.12.0"]
                 [clojure-csv/clojure-csv "2.0.2"]
                 [com.taoensso/encore "2.126.2"]
                 [com.taoensso/timbre "4.11.0-alpha2" :exclusions [cheshire
                                                                   com.taoensso/encore
                                                                   com.taoensso/nippy
                                                                   com.draines/postal
                                                                   com.mchange/c3p0
                                                                   org.clojure/java.jdbc
                                                                   org.julienxx/clj-slack]]
                 [prismatic/plumbing "0.6.0"]
                 [prismatic/schema "1.1.10"]])
