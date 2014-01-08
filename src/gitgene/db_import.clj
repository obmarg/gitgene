(ns gitgene.db-import
  (:require [clojure.string :as string]
            [clojurewerkz.neocons.rest.nodes :as nn]
            [clojurewerkz.neocons.rest.relationships :as nrel]
            [clojurewerkz.neocons.rest.labels :as nl]
            [clojurewerkz.neocons.rest.cypher :as cy]
            [clojurewerkz.neocons.rest.records :as records]
            [clojure.core.async :as async]))

(defn- line-relationship
  [kind]
  ((keyword kind) {:added :ADDED_LINE
                   :removed :REMOVED_LINE}))

; TODO: Maybe get rid of this.  Having a cache is actually more
;       of a pain in the arse than it's worth just now.
;       A lesson in naming things and cache invalidation.
(def ^:private file-cache (agent {}))

(defn- find-or-create
  "Finds or creates a node with label & properties"
  [label key-prop properties]
  (if-let [node (first (nl/get-all-nodes label
                                         key-prop
                                         (key-prop properties)))]
    node
    (doto
      (nn/create properties)
      (nl/add label))))

(defn- file-from-db
  "Finds or creates a file node in the db"
  [filename]
  (find-or-create
   "File" :path {:name (last (string/split filename #"/"))
                 :path filename
                 :type (last (string/split filename #"\."))}))

(defn- find-author
  "Finds or creates an author in the db"
  [{:keys [author email]}]
  (find-or-create
   "User" :email {:email email
                  :name author}))

(defn- find-file
  "Finds the node for a file, or creates it if it doesn't exist.
   Uses an agent to ensure proper coordination."
  [filename]
  {:pre [(not (nil? filename))]}
  (if-let [node (@file-cache filename)]
    node
    (do
      (send-off file-cache
                (fn [oldval]
                  (if (oldval filename)
                    oldval
                    (assoc oldval filename (file-from-db filename)))))
      (await-for 500 file-cache)
      (@file-cache filename))))

; Stolen from https://groups.google.com/d/topic/clojure-neo4j/4-OfBAae9qM/discussion
(defn- cypher-convert-value [value]
  "Converts a cypher value into a neocons node/path/rel"
  (cond
   (:type value)   (records/instantiate-rel-from value)
   (:length value) (records/instantiate-path-from value)
   (:self value)   (records/instantiate-node-from value)
   :else           value))

(defn- except-if-nil
  [value message]
  (if-not value
    (throw (Exception. message))
    value))

(defn- find-line
  "Finds the node for a line"
  [{:keys [filename contents]}]
  ; TODO: At present this probably won't handle
  ;       branches too well, beacuse a line can be removed more than
  ;       once on different branches.  Fix this.
  ; TODO: Also won't handle files with multiple lines of the same contents
  (-> (cy/tquery "MATCH (f:File)-[:CONTAINS]->(l:Line)
                  WHERE f.path={path} AND
                        not(l<-[:REMOVED_LINE]-()) AND
                        l.contents={contents}
                  RETURN l AS line"
                {:path filename
                 :contents contents})
      first
      (get "line")
      cypher-convert-value
      (except-if-nil (str "Could not find line in " filename "containing" contents))))

(defn- relate-line-to-commit
  "Labels a line node & adds its relationships"
  [commit kind line]
  (nrel/create commit line kind))

(defn- add-author-to-commit
  "Finds an author and relates the commit to it."
  [commit]
  (nrel/create commit (find-author (:data commit)) :AUTHOR))

(defn- process-new-line
  "Labels a line node & links it to its file"
  [line-node line]
  (nl/add line-node "Line")
  (nrel/create (find-file (:filename line)) line-node :CONTAINS))

; TODO: Need to filter out certain properties before setting on the
;       server.

; eager-map is not the best name, since it doesn't return a list like map
; but naming things is hard.
(def ^:private eager-map (comp dorun map))

(defn- process-commit
  "Labels a commit node and adds the appropriate lines.
   Intended to be used from witin a channel."
  [commit lines]
  (try
    (nl/add commit "Commit")
    (add-author-to-commit commit)
    (let [{:keys [added removed]} (group-by :kind lines)
          added-nodes (nn/create-batch added)
          removed-nodes (map find-line removed)
          relate-line #(nrel/create commit %1 %2)]
      (eager-map process-new-line added-nodes added)
      (eager-map #(relate-line % :ADDED_LINE) added-nodes)
      (eager-map #(relate-line % :REMOVED_LINE) removed-nodes))
    (catch Exception e (throw (Exception.
                               (str "Error processing commit" (:data commit))
                               e)))))



(defn add-commits
  "Bulk adds some commits to the database.
   Expects [[commit-details, lines]...]"
  [commits]
  (let [nodes (nn/create-batch (map first commits))]
    (eager-map process-commit nodes (map second commits))))

;(def test-commits [[{:name "TEST"} #{{:name "Line 1" :kind :added :linenum 1
;                                      :filename "test.txt"}
;                                     {:name "Line 2" :kind :added :linenum 2
;                                      :filename "test.txt"}}]
;                   [{:name "TEST2"} #{{:name "Line 2-2" :kind :removed :linenum 2
;                                       :filename "test.txt"}}]])
;
;(add-commits test-commits)
