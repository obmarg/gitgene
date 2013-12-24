(ns stubbly.core-test
  (:use midje.sweet
        stubbly.core)
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

(fact "Split diff splits diffs into seqs of (header, lines)"
  (split-diff test-diff) => (->> test-diff
                                 (clojure.string/split-lines)
                                 (split-at 10)
                                 (map #(split-at 1 %))))
