(defproject gitgene "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :source-paths ["src/clj"]
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [clj-jgit "0.6.4"]
                 [clojurewerkz/neocons "2.0.0"]
                 [org.clojure/core.match "0.2.0"]
                 [compojure "1.1.6"]
                 [org.clojure/clojurescript "0.0-2138"]
                 [ring "1.2.1"]
                 [hiccup "1.0.4"]
                 [om "0.1.5"]
                 [com.cemerick/shoreleave-remote-ring "0.0.2"]
                 [shoreleave/shoreleave-remote "0.3.0"]]
  :profiles {:dev {:dependencies [[midje "1.5.1"]
                                  #_[org.neo4j/neo4j-kernel "2.0.0"]
                                  [org.neo4j/neo4j-kernel "2.0.0"
                                   :classifier "tests"]
                                  [org.neo4j/neo4j-cypher "2.0.0"]
                                  [org.clojure/core.async "0.1.267.0-0d7780-alpha"]]}}
  :plugins [[lein-cljsbuild "1.0.1"]
            [lein-ring "0.8.7"]]
  :cljsbuild {
    :builds [{:source-paths ["src/cljs"]
              :compiler {:output-to "resources/public/js/main.js"
                         :output-dir "out"
                         :optimizations :none
                         :pretty-print true
                         :source-map true}}]}
  :ring {:handler gitgene.routes/app}
  :main gitgene.core)
