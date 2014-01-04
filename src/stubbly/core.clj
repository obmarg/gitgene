(ns stubbly.core
  (:gen-class)
  (:require [stubbly.git :as git]
            [clojurewerkz.neocons.rest :as nr]))

(nr/connect! "http://localhost:7474/db/data/")

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
