(ns gepetto.workers
  (:require [resque-clojure.core :as resque]))

(defn load-resque
  []
  (resque/configure {:host "localhost" :port 6379}))

(def enqueue resque/enqueue)

(def start resque/start)

(def stop resque/stop)
