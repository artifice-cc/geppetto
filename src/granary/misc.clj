(ns granary.misc
  (:require [clojure.java.jdbc :as jdbc])
  (:use [korma.db])
  (:use [korma.config])
  (:import (java.util Date))
  (:import (java.text SimpleDateFormat)))

(def granary-db (ref nil))

(defn set-granary-db
  [dbhost dbname dbuser dbpassword]
  (set-delimiters "`")
  (dosync
   (alter granary-db
          (constantly
           (create-db
            (mysql {:db dbname :user dbuser :password dbpassword :host dbhost}))))))

(defmacro with-db  
  "Execute all queries within the body using the given db spec"
  [db & body]
  `(jdbc/with-connection (get-connection ~db)
     ~@body))

(defn format-date-ms
  [ms]
  (.format (SimpleDateFormat. "yyyy-MM-dd HH:mm:ss") (Date. (long ms))))
