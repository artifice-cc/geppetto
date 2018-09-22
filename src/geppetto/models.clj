(ns geppetto.models
  (:use [korma.core]))

(defentity parameters
  (pk :paramid))

(defentity runs
  (pk :runid)
  (belongs-to parameters {:fk [:paramid]}))

(defentity analyses
  (pk :analysisid))

(defentity run-analyses
  (table :run_analyses)
  (pk :runanalysisid)
  (belongs-to runs {:fk [:runid]})
  (belongs-to analyses {:fk [:analysisid]}))

(defentity template-analyses
  (table :template_analyses)
  (pk :templateid)
  (belongs-to runs {:fk [:runid]}))

(defentity graphs
  (pk :graphid))

(defentity run-graphs
  (table :run_graphs)
  (pk :rungraphid)
  (belongs-to runs {:fk [:runid]})
  (belongs-to graphs {:fk [:graphid]}))

(defentity template-graphs
  (table :template_graphs)
  (pk :templateid)
  (belongs-to runs {:fk [:runid]}))

(defentity table-fields
  (table :table_fields)
  (pk :tfid)
  (belongs-to runs {:fk [:runid]}))

(defn text-field
  [val]
  (cond (= org.h2.jdbc.JdbcClob (class val))
        (slurp (.getCharacterStream val))
        :else
        val))
