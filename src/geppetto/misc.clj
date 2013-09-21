(ns geppetto.misc
  (:use [korma.db :only [defdb mysql]])
  (:use [korma.config])
  (:import (java.util Date))
  (:import (java.text SimpleDateFormat))
  (:require [geppetto.workers :as workers]))

(def quiet-mode (ref nil))

(defn setup-geppetto
  [dbhost dbport dbname dbuser dbpassword quiet?]
  (workers/load-resque)
  (set-delimiters "`")
  (defdb geppetto-db (mysql {:db dbname :port dbport :user dbuser :password dbpassword :host dbhost}))
  (dosync (alter quiet-mode (constantly quiet?))))

(defn format-date-ms
  [ms]
  (.format (SimpleDateFormat. "yyyy-MM-dd HH:mm:ss") (Date. (long ms))))
