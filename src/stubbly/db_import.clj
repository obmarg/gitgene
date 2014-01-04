(ns stubbly.db-import
  (:require [clojurewerkz.neocons.rest.nodes :as nn]
            [clojurewerkz.neocons.rest.relationships :as nrel]
            [clojurewerkz.neocons.rest.labels :as nl]
            [clojure.core.async :as async]))


(def ^:private line-relationship
  {:added :ADDED_LINE
   :removed :REMOVED_LINE})

(def ^:private wait-all!! (partial map async/<!!))
(def ^:private wait-all! (partial map async/<!))

(defn- process-line
  "Labels a line node & adds its relationships"
  [commit line-node line]
  (nl/add line-node "Line")
  (nrel/create commit line-node (line-relationship (:kind line)))
  nil)
  ; TODO:
  ;   Add the CONTAINS relationship (which requires getting the file)

; TODO: Need to filter out certain properties before setting on the
;       server.

(defn- process-commit
  "Labels a commit node and adds the appropriate lines.
   Intended to be used from witin a channel."
  [commit lines]
  (nl/add commit "Commit")
  ; TODO: This is a bit shit - need a "find or create" for lines,
  ;       because removed lines should already be in the database.
  ;       This might mean we need a cache of lines (at least ideally)
  ;
  ;       Also need to be able to locate lines in the database by
  ;       finding lines that
  (let [line-nodes (nn/create-batch lines)]
    (wait-all!!
     (for [[node line] (map vector line-nodes lines)]
         (async/go (process-line commit node line))))))

(defn add-commits
  "Bulk adds some commits to the database.
   Expects [[commit-details, lines]...]"
  [commits]
  (let [nodes (nn/create-batch (map first commits))]
    (wait-all!!
     (for [[node lines] (map vector nodes (map second commits))]
       ; TODO: async/thread or future may be better here...
      (async/go (process-commit node lines))))))

(def test-commits [[{:name "TEST"} #{{:name "Line 1" :kind :added}
                                     {:name "Line 2" :kind :removed}}]])

; (add-commits test-commits)
