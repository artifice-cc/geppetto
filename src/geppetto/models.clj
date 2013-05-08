(ns geppetto.models
  (:use [korma.core]))

(defentity parameters
  (pk :paramid))

(defentity runs
  (pk :runid)
  (belongs-to parameters {:fk :paramid}))


