(ns stubbly.git
  (:use [clj-jgit.porcelain :only [load-repo git-log]]
        [clj-jgit.querying :only [changed-files rev-list commit-info
                                  commit-info-without-branches
                                  find-rev-commit]]
        [clj-jgit.internal :only [new-rev-walk]]
        [clojure.core.match :only [match]])
  (:import [java.io ByteArrayOutputStream]
           [org.eclipse.jgit.diff DiffFormatter RawTextComparator]
           [org.eclipse.jgit.api Git]
           [org.eclipse.jgit.revwalk RevCommit]
           [org.eclipse.jgit.treewalk EmptyTreeIterator CanonicalTreeParser])
  (:require [clojure.string]
            [stubbly.levenshtein :as levenshtien]))

(declare diff-for-commit diff-split-with)

(def repo-path "/Users/grambo/src/rolepoint-app")

(def repo (load-repo repo-path))

(def revisions (rev-list repo))

(commit-info repo (last (git-log repo)))

(count revisions)
(changed-files repo (last revisions))
(commit-info repo (last revisions))

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
  (let [stream (ByteArrayOutputStream.)
        formatter (DiffFormatter. stream)
        obj-reader (.getObjectReader (new-rev-walk repo))]
    (doto formatter
      (.setRepository (.getRepository repo))
      (.setDiffComparator RawTextComparator/DEFAULT)
      (.setDetectRenames false))
    (if-let [parent (first (.getParents rev-commit))]
      (.format formatter parent rev-commit)
      (.format formatter
               (EmptyTreeIterator.)
               (CanonicalTreeParser. nil obj-reader (.getTree rev-commit))))
    (.toString stream)))
    ; TODO: Write the else branch of if-let (for the first commit)
    ;       Really just need to call format with an EmptyTreeIterator
    ;       and a CanonicalTreeParser as explained here:
    ;       http://stackoverflow.com/questions/12493916/getting-commit-information-from-a-revcommit-object-in-jgit

; (def split-diffs (split-diff (diff-for-commit repo revision)))

(defn split-diff-sections
  [input]
  (->> input
       (partition-by-re section-header-regexp)
       (drop 1)
       (partition 2)
       (flatten)))

; (split-diff-sections (second split-diffs))
; (map split-diff-sections split-diffs)

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

; (def section (split-diff-sections (first split-diffs)))
;(parse-section section)

(defn parse-file-diff
  "Parses a single diff for a file into a description hash"
  [[header-line & other-lines]]
  (let [sections (split-diff-sections other-lines)]
    {:file (second (re-matches header-regexp header-line))
     :lines (parse-section sections)}))

; (parse-file-diff (first split-diffs))

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

; (parse-diff (diff-for-commit repo revision))

(defn rev->lines
  "Takes a repository & revision, returns a set of changed line hashmaps"
  [repo commit]
  (try
    (parse-diff (diff-for-commit repo commit))
    (catch Exception e (throw (Exception. (str commit) e)))))


;(commit-info repo (last revisions))
;(diff-for-commit repo (last revisions))

(def ^:private metadata-fields [:message :author :email :merge :time :id])

(defn- extract-metadata
  [repo rev-commit]
  (zipmap metadata-fields
          ; TODO: Can switch to commit-info-without-branches if need speed up
          (map (commit-info repo rev-commit) metadata-fields)))

;(extract-metadata repo (last revisions))

(defn import-repo [path]
  "Loads a repository from a path and returns a lazy seq of hashes"
  (let [repo (load-repo path)]
    (->> (git-log repo)
         ; Note: commit-info includes file added/removed info, which
         ;       could possibly be useful (particularly the removal)
         reverse
         (map (juxt extract-metadata rev->lines) (repeat repo)))))

;(import-repo repo-path)
;(import-repo "/Users/grambo/src/stubbly")

;(commit-info repo (first (.getParents (find-rev-commit repo (new-rev-walk repo) "c39e7d"))))
