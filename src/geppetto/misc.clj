(ns geppetto.misc
  (:require [clojure.java.jdbc :as jdbc])
  (:use [korma.db :only [create-db mysql]])
  (:use [korma.config])
  (:import (java.util Date))
  (:import (java.text SimpleDateFormat)))

(def geppetto-db (ref nil))
(def geppetto-dbhost (ref nil))
(def geppetto-dbname (ref nil))
(def geppetto-dbuser (ref nil))
(def geppetto-dbpassword (ref nil))

(defn set-geppetto-db
  [dbhost dbname dbuser dbpassword]
  (set-delimiters "`")
  (dosync
   (alter geppetto-db
          (constantly
           (create-db
            (mysql {:db dbname :user dbuser :password dbpassword :host dbhost}))))
   (alter geppetto-dbhost (constantly dbhost))
   (alter geppetto-dbname (constantly dbname))
   (alter geppetto-dbuser (constantly dbuser))
   (alter geppetto-dbpassword (constantly dbpassword))))

(defmacro with-db  
  "Execute all queries within the body using the given db spec"
  [db & body]
  `(jdbc/with-connection (korma.db/get-connection ~db)
     ~@body))

(defn format-date-ms
  [ms]
  (.format (SimpleDateFormat. "yyyy-MM-dd HH:mm:ss") (Date. (long ms))))