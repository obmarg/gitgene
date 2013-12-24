(ns stubbly.core
  (:gen-class)
  (:use [clj-jgit.porcelain :only [load-repo]]
        [clj-jgit.querying :only [changed-files rev-list]])
  (:import [java.io ByteArrayOutputStream]
           [org.eclipse.jgit.diff DiffFormatter RawTextComparator]
           [org.eclipse.jgit.api Git]
           [org.eclipse.jgit.revwalk RevCommit])
  (:require [clojure.string]))

(declare diff-for-commit)

(def repo-path "/Users/grambo/src/rolepoint-app")


(def repo (load-repo repo-path))

(def revisions (rev-list repo))

(def revision (first revisions))

(changed-files repo revision)

(defn- diff-chunks
  "Splits a diff into it's changed chunks"
  [diff]
  ())

(defn spliterate
  "Specialised iterate for splitting a seq on irregular boundaries.
   Takes a fn of data -> [item, rest-data].
   Returns a seq of the items obtained from iterating this fn over arg."
  [f arg]
  (->> (iterate (comp f second) [nil arg])
       (drop 1)
       (map first)))

(defn split-diff
  [data]
  (->> data
       clojure.string/split-lines
       (spliterate diff-split-with)
       (take-while first)))

(def not-diff-header?
  (complement #(re-matches #"^diff --git.*$" %)))

(defn- diff-split-with
  "Wrapper around split-with that strips out the first couple of lines
   for purposes of the predicate."
  [[line1 line2 & remainder]]
  (let [[item other] (split-with not-diff-header? remainder)]
    [(concat [line1 line2] item) other]))

(split-diff (diff-for-commit repo revision))

; TODO: Could possibly refactor the splitting by using partition-by
;       or reduce as in
;       http://stackoverflow.com/questions/2538326/parsing-data-with-clojure-interval-problem

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

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
