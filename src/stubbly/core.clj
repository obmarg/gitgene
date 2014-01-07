(ns stubbly.core
  (:gen-class)
  (:require [stubbly.git :as git]
            [stubbly.db-import :as db-import]
            [clojurewerkz.neocons.rest :as nr]))

(nr/connect! "http://localhost:7474/db/data/")

(def import-to-db
  (comp db-import/add-commits git/import-repo))

; (def commit (nth (git/import-repo "/Users/grambo/src/stubbly") 4))

;(first commit)
; (map println (sort-by :linenum (second commit)))

;(import-to-db "/Users/grambo/src/stubbly")

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
