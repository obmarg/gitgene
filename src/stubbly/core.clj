(ns stubbly.core
  (:gen-class)
  (:use [clj-jgit.porcelain :only [load-repo]]
        [clj-jgit.querying :only [changed-files rev-list]])
  (:import [java.io ByteArrayOutputStream]
           [org.eclipse.jgit.diff DiffFormatter RawTextComparator]
           [org.eclipse.jgit.api Git]
           [org.eclipse.jgit.revwalk RevCommit])
  (:require [clojure.string]))

(declare diff-for-commit diff-split-with)

(def repo-path "/Users/grambo/src/rolepoint-app")

(def repo (load-repo repo-path))

(def revisions (rev-list repo))

(def revision (first revisions))

(changed-files repo revision)

(def diff-header?
  (partial re-matches #"^diff --git.*$"))

(defn split-diff
  [data]
  (->> data
       clojure.string/split-lines
       (partition-by diff-header?)
       (partition 2)))

(defn- diff-for-commit
  [^Git repo ^RevCommit rev-commit]
  (if-let [parent (first (.getParents rev-commit))]
    (let [stream (ByteArrayOutputStream.)]
      (doto
        (DiffFormatter. stream)
        (.setRepository (.getRepository repo))
        (.setDiffComparator RawTextComparator/DEFAULT)
        (.setDetectRenames false)
        (.format parent rev-commit))
      (.toString stream))
    ; TODO: Write the else branch of if-let (for the first commit)
    ))

(split-diff (diff-for-commit repo revision))

; Ok, so this gives me a seq of individual diff chunks.
; From this I need to extract:
;   - File details (trivial)
;   - Lines added (easy enough)
;   - Lines removed (easy enough)
;   - Lines changed (HARD)
;     - Probably want to just do added vs removed first,
;       then determine what was "changed" afterwards.
;       Though not sure - context might be important for
;       finding changes.

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
