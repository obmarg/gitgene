(ns stubbly.core
  (:gen-class)
  (:use [clj-jgit.porcelain :only [load-repo]]
        [clj-jgit.querying :only [changed-files rev-list]])
  (:import [java.io ByteArrayOutputStream]
           [org.eclipse.jgit.diff DiffFormatter RawTextComparator]
           [org.eclipse.jgit.api Git]
           [org.eclipse.jgit.revwalk RevCommit])
  (:require [clojure.string]))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
