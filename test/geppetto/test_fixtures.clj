(ns geppetto.test-fixtures
  (:import [java.net URI])
  (:require [clojure.string :as str])
  (:use [geppetto.misc])
  (:use [geppetto.parameters])
  (:use [geppetto.models])
  (:use [korma db core config]))

(defn establish-params
  []
  (new-parameters {:problem "Testing"
                   :name "test-1"
                   :control "{:foo [1 2]}"
                   :description "testing params"}))

(defn in-memory-db
  [f]
  (dosync (alter geppetto-db (constantly {:classname "org.h2.Driver"
                                          :subprotocol "h2"
                                          :subname "mem:test;DB_CLOSE_DELAY=-1"})))
  (set-delimiters "")
  (set-naming {:keys str/lower-case})
  (with-db @geppetto-db
    (exec-raw (slurp "testdb.sql")))
  (establish-params)
  (f))

