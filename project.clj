(defproject cc.artifice/granary "1.0.1"
  :description "Backend support such as database connectivity for Sisyphus, Retrospect, Chivalry, and other projects that use the same data."
  :url "http://artifice.cc/granary"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [org.clojure/java.jdbc "0.2.3"]
                 [korma "0.3.0-RC2"]
                 [mysql/mysql-connector-java "5.1.6"]])
