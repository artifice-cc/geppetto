(defproject cc.artifice/geppetto "3.0.1-SNAPSHOT"
  :description "Backend support for experimental work."
  :url "http://geppetto.artifice.cc"
  :license {:name "MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/tools.cli "0.2.2"]
                 [org.clojure/tools.macro "0.1.2"]
                 [org.clojure/core.cache "0.6.3"]
                 [myguidingstar/clansi "1.3.0"]
                 [cc.artifice/propertea "1.4.1"]
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
  :codox {:sources ["src"]
          :src-dir-uri "http://github.com/artifice-cc/geppetto/blob/master/"
          :src-linenum-anchor-prefix "L"
          :output-dir "docs/codox"
          :marginalia-dir "../marginalia"
          :gossip-dir "../gossip"
          :fnviz-dir "../fnviz"}
  :marginalia {:javascript ["http://cdn.mathjax.org/mathjax/latest/MathJax.js?config=TeX-AMS-MML_HTMLorMML"]}
  :gossip {:target "docs/gossip"})
