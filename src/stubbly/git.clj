(ns stubbly.git
  (:use [clj-jgit.porcelain :only [load-repo]]
        [clj-jgit.querying :only [changed-files rev-list]]
        [clojure.core.match :only [match]])
  (:import [java.io ByteArrayOutputStream]
           [org.eclipse.jgit.diff DiffFormatter RawTextComparator]
           [org.eclipse.jgit.api Git]
           [org.eclipse.jgit.revwalk RevCommit])
  (:require [clojure.string]
            [stubbly.levenshtein :as levenshtien]))

(declare diff-for-commit diff-split-with)

(def repo-path "/Users/grambo/src/rolepoint-app")

(def repo (load-repo repo-path))

(def revisions (rev-list repo))

(def revision (first revisions))

(changed-files repo revision)

(def header-regexp #"^diff --git a/(.*) b/(.*)$")
(def section-header-regexp #"^@@ -(\d+),(\d+) \+(\d+),(\d+) @@$")

(defn- partition-by-re
  [regexp input]
  (partition-by #(re-matches regexp %) input))

(defn split-diff
  [input]
  (->> input
       clojure.string/split-lines
       (partition-by-re header-regexp)
       (partition 2)
       (map flatten)))

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

(def split-diffs (split-diff (diff-for-commit repo revision)))

(defn split-diff-sections
  [input]
  (->> input
       (partition-by-re section-header-regexp)
       (drop 1)
       (partition 2)
       (flatten)))

(split-diff-sections (second split-diffs))
(map split-diff-sections split-diffs)

(defn- parse-line
  [[first-char & rest-line]]
  (let [kind (match [first-char]
                    [\+]  :added
                    [\-]  :removed
                    :else :context)]
    {:contents (if (= kind :context)
                 (str first-char (apply str rest-line))
                 (apply str rest-line))
     :kind kind}))

(defn- remove-kind [kind lines]
  (remove #(= kind (:kind %)) lines))

(defn parse-section
  "Parses a section into a list of changed lines"
  [[section-header & diff-lines]]
  (let [[_ a-start a-len b-start b-len] (re-matches section-header-regexp
                                                    section-header)
        lines (map parse-line diff-lines)
        [added removed] ((juxt #(remove-kind :removed %)
                              #(remove-kind :added %))
                         lines)
        linenum-fn (fn [lines start-index]
                     (map-indexed
                      #(assoc %2 :linenum (+ %1 (Integer. start-index)))
                      lines))]
    (set (remove #(= :context (:kind %))
      (concat (linenum-fn added b-start)
              (linenum-fn removed a-start))))))

(def section (split-diff-sections (first split-diffs)))

(parse-section section)
(defn parse-file-diff
  "Parses a single diff for a file into a description hash"
  [[header-line & other-lines]]
  (let [sections (split-diff-sections other-lines)]
    {:file (second (re-matches header-regexp header-line))
     :lines (parse-section sections)}))

(parse-file-diff (first split-diffs))

(defn parse-diff
  "Takes a text diff, returns a set of changed line hashmaps.
   {:filename :kind :linenum :contents"
  [diff]
  (->> diff
       split-diff
       (map parse-file-diff)
       (mapcat (fn [file] (map
                           #(assoc % :filename (:file file))
                           (:lines file))))
       set))

(parse-diff (diff-for-commit repo revision))

(def repo-and-rev->dict
  "Takes a repository & revision, returns a set of changed line hashmaps"
  (comp parse-diff diff-for-commit))

(repo-and-rev->dict repo revision)
