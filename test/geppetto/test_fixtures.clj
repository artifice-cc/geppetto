(ns geppetto.test-fixtures
  (:import [java.net URI])
  (:use [clojure.java.shell :only [sh]])
  (:require [clojure.string :as str])
  (:use [geppetto.misc])
  (:use [geppetto.parameters])
  (:use [geppetto.models])
  (:use [geppetto.random])
  (:use [korma db core])
  (:use [korma.sql.engine :only [*bound-options*]])
  (:require [taoensso.timbre :as timbre]))

(defn establish-params
  []
  (new-parameters {:problem "Testing"
                   :name "test-1"
                   :control "{:foo [1 2]}"
                   :description "testing params"}))

(defn in-memory-db
  [f]
  (dosync (alter geppetto-db (constantly (h2 {:db "mem:test"
                                              :options {:naming {:keys str/lower-case :fields str/lower-case}
                                                        :delimiters ["`" "`"]}}))))
  (let [sql (slurp "tables.sql")
        h2sql (-> sql
                  (str/replace #"`id` int\(11\) NOT NULL," "`id` int(11) NOT NULL AUTO_INCREMENT,")
                  (str/replace #"(UNIQUE)? (INDEX|KEY) `.+` \(`[a-zA-Z0-9_]+`(\(\d+\))?( [DASC]+)?\),?" "")
                  (str/replace #"(UNIQUE)? (INDEX|KEY) `.+` \(`[a-zA-Z0-9_]+`(\(\d+\))?( [DASC]+)?\s*,\s*`[a-zA-Z0-9_]+`(\(\d+\))?( [DASC]+)?\),?" "")
                  (str/replace #"`([^`]*)`" "$1")
                  (str/replace #"CHARACTER SET '[^']+'" "")
                  (str/replace #"ENGINE=InnoDB" "")
                  (str/replace #"DEFAULT CHARSET=[a-zA-Z0-9]+" "")
                  (str/replace #",\s*\)\s*;" ");"))]
    (with-db @geppetto-db
      (exec-raw h2sql)
      (establish-params)
      (f))))

(defn travis-mysql-db
  [f]
  (sh "mysql" "-u" "travis" "geppetto_test" :in (slurp "tables.sql"))
  (dosync (alter geppetto-db (constantly (mysql {:db "geppetto_test" :user "travis" :password ""
                                                 :options {:naming {:keys str/lower-case}
                                                           :delimiters ["`" "`"]}}))))
  (establish-params)
  (f))

(defn setup-random-seed
  [f]
  (alter-var-root (var rgen) (constantly (new-seed 0)))
  (f))

(defn quiet-mode
  [f]
  (timbre/set-level! :warn)
  (f))
