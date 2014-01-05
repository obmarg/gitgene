(ns stubbly.db-import
  (:require [clojure.string :as string]
            [clojurewerkz.neocons.rest.nodes :as nn]
            [clojurewerkz.neocons.rest.relationships :as nrel]
            [clojurewerkz.neocons.rest.labels :as nl]
            [clojure.core.async :as async]))


(def ^:private line-relationship
  {:added :ADDED_LINE
   :removed :REMOVED_LINE})

(def ^:private wait-all!! (partial map async/<!!))
(def ^:private wait-all! (partial map async/<!))

(def ^:private file-cache (agent {}))

(defn- file-from-db
  "Finds or creates a file node in the db"
  [filename]
  (if-let [node (first (nl/get-all-nodes "File" :path filename))]
    node
    (let [node (nn/create {:name (last (string/split filename #"/"))
                           :path filename
                           :type (last (string/split filename #"\."))})]
      (nl/add node "File")
      node)))

(defn- find-file
  "Finds the node for a file, or creates it if it doesn't exist.
   Uses an agent to ensure proper coordination."
  [filename]
  (if-let [node nil]
    node
    (do
      (send-off file-cache
                (fn [oldval]
                  (if (oldval filename)
                    oldval
                    (assoc oldval filename (file-from-db filename)))))
      (await-for 500 file-cache)
      (@file-cache filename))))

(defn- process-line
  "Labels a line node & adds its relationships"
  [commit line-node line]
  (nl/add line-node "Line")
  (nrel/create commit line-node (line-relationship (:kind line)))
  (nrel/create (find-file (:filename line)) line-node :CONTAINS)
  nil)

; TODO: Need to filter out certain properties before setting on the
;       server.

(defn- process-commit
  "Labels a commit node and adds the appropriate lines.
   Intended to be used from witin a channel."
  [commit lines]
  (nl/add commit "Commit")
  ; TODO: This is a bit shit - need a "find or create" for lines,
  ;       because removed lines should already be in the database.
  ;       This might mean we need a cache of lines (at least ideally).
  ;
  ;       Also need to be able to locate lines in the database somehow,
  ;       probably using linenum & file.
  (let [line-nodes (nn/create-batch lines)]
    (wait-all!!
     (for [[node line] (map vector line-nodes lines)]
       ; TODO: Seperate threads per line is almost definitely
       ;       overkill, might even slow things down.
       ;       Maybe look into making this better...
       (async/thread (process-line commit node line))))))

(defn add-commits
  "Bulk adds some commits to the database.
   Expects [[commit-details, lines]...]"
  [commits]
  (let [nodes (nn/create-batch (map first commits))]
    (wait-all!!
     (for [[node lines] (map vector nodes (map second commits))]
      (async/thread (process-commit node lines))))))

(def test-commits [[{:name "TEST"} #{{:name "Line 1" :kind :added}
                                     {:name "Line 2" :kind :removed}}]])

(add-commits test-commits)
