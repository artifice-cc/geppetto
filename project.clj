(defproject cc.artifice/geppetto "3.2.2"
  :description "Backend support for experimental work."
  :url "http://geppetto.artifice.cc"
  :license {:name "MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/tools.cli "0.3.5"]
                 [org.clojure/tools.macro "0.1.5"]
                 [org.clojure/core.cache "0.6.5"]
                 [myguidingstar/clansi "1.3.0"]
                 [cc.artifice/propertea "1.4.1"]
                 [org.clojars.ejschoen/korma "0.4.5"]
                 [mysql/mysql-connector-java "6.0.3"]
                 [com.h2database/h2 "1.4.195"]
                 [org.apache.commons/commons-math3 "3.6.1"]
                 [clj-time "0.12.0"]
                 [clojure-csv/clojure-csv "2.0.2"]
                 [com.taoensso/timbre "4.6.0"]
                 [prismatic/plumbing "0.5.4"]
                 [prismatic/schema "1.1.9"]]
  :profiles {:dev {:dependencies [[org.clojure/tools.namespace "0.2.11"]
                                  [org.clojure/tools.nrepl "0.2.13"]]}}
  :codox {:sources ["src"]
          :src-dir-uri "http://github.com/artifice-cc/geppetto/blob/master/"
          :src-linenum-anchor-prefix "L"
          :output-dir "docs/codox"
          :marginalia-dir "../marginalia"
          :gossip-dir "../gossip"
          :fnviz-dir "../fnviz"}
  :marginalia {:javascript ["http://cdn.mathjax.org/mathjax/latest/MathJax.js?config=TeX-AMS-MML_HTMLorMML"]}
  :gossip {:target "docs/gossip"})
