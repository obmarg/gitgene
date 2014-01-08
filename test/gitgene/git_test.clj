(ns gitgene.git-test
  (:use midje.sweet
        gitgene.git)
  (:require [clojure.string]))

(def ^:private test-diff
  "diff --git a/project.clj b/project.clj
index ca84a0c..19b1a3b 100644
--- a/project.clj
+++ b/project.clj
@@ -6,4 +6,5 @@
   :dependencies [[org.clojure/clojure \"1.5.1\"]
                  [clj-jgit \"0.6.4\"]
                  [clojurewerkz/neocons \"2.0.0\"]]
+  :profiles {:dev {:dependencies [[midje \"1.5.1\"]]}}
   :main stubbly.core)
diff --git a/test/stubbly/core_test.clj b/test/stubbly/core_test.clj
index d75c820..6297bd9 100644
--- a/test/stubbly/core_test.clj
+++ b/test/stubbly/core_test.clj
@@ -1,5 +1,5 @@
 (ns stubbly.core-test
-  (:use clojure.test
+  (:use midje.sweet
         stubbly.core))

 (deftest a-test")

(def ^:private test-diff-lines
  (clojure.string/split-lines test-diff))

(fact "split-diff splits diffs into seqs of (header, lines)"
  (split-diff test-diff) => (split-at 10 test-diff-lines))

(fact "split-diff-sections splits out a seq of sections"
  (split-diff-sections (take 10 test-diff-lines))
      => (drop 4 (take 10 test-diff-lines)))

(fact "parse-section returns added & removed lines with indexes"
  (parse-section (drop 14 test-diff-lines))
      => #{{:kind :removed
            :linenum 2
            :contents "  (:use clojure.test"}
           {:kind :added
            :linenum 2
            :contents "  (:use midje.sweet"}})

(fact "parse-diff takes a diff and returns added & removed lines with indexes"
 (parse-diff test-diff)
      => #{{:kind :removed
            :linenum 2
            :filename "test/stubbly/core_test.clj"
            :contents "  (:use clojure.test"}
           {:kind :added
            :linenum 2
            :filename "test/stubbly/core_test.clj"
            :contents "  (:use midje.sweet"}
           {:kind :added
            :linenum 9
            :filename "project.clj"
            :contents "  :profiles {:dev {:dependencies [[midje \"1.5.1\"]]}}"}})
