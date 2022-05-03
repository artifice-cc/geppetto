(ns geppetto.misc
  (:use [korma.db :only [mysql]])
  ;;(:use [korma.config])
  (:import (java.util Date))
  (:import (java.text SimpleDateFormat))
  (:use [taoensso.timbre]))

(def geppetto-db (ref nil))

(defn setup-geppetto
  [dbhost dbport dbname dbuser dbpassword quiet?]
  (if quiet? (set-level! :warn) (set-level! :info))
  (dosync (alter geppetto-db (constantly (mysql {:db dbname :port dbport :user dbuser
                                                 :password dbpassword :host dbhost
                                                 :delimeters "`"})))))

(defn format-date-ms
  [ms]
  (.format (SimpleDateFormat. "yyyy-MM-dd HH:mm:ss") (Date. (long ms))))

(defn get-insert-id
  [result]
  (or (:generated_key result)
      (:GENERATED_KEY result)
      (get result (keyword "scope_identity()"))
      (first (vals result))))
