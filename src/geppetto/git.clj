(ns geppetto.git
  (:use [clojure.string :only [split-lines trim]])
  (:use [clojure.java.shell :only [sh]])
  (:use [clj-time coerce format]))

(def custom-formatter (formatter "YYYY-MM-dd hh:mm:ss"))

(defn get-commit-date
  [git pwd commit]
  (let [git-output (:out (sh git (format "--work-tree=%s" pwd)
                             "show" "--format=raw" commit))
        timestamp (second (re-find #"committer .* (\d+) [-+]\d{4}" git-output))]
    (unparse custom-formatter (from-long (* 1000 (Long/parseLong timestamp))))))

(defn git-meta-info
  [git pwd]
  (let [[out _ _ _ & msg] (split-lines (:out (sh git (format "--work-tree=%s" pwd)
                                                 "log" "-n" "1")))
        branch (trim (subs (:out (sh git (format "--work-tree=%s" pwd)
                                     "branch" "--contains")) 2))
        commit (subs out 7)]
    {:commit commit
     :commitdate (get-commit-date git pwd commit)
     :commitmsg (apply str (interpose "\n" (map (fn [s] (subs s 4)) (filter not-empty msg))))
     :branch branch}))
