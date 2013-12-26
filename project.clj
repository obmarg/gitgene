(defproject stubbly "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [clj-jgit "0.6.4"]
                 [clojurewerkz/neocons "2.0.0"]
                 [org.clojure/core.match "0.2.0"]]
  :profiles {:dev {:dependencies [[midje "1.5.1"]
                                  [org.neo4j/neo4j-kernel "2.0.0"]
                                  [org.neo4j/neo4j-kernel "2.0.0"
                                   :classifier "tests"]
                                  [org.neo4j/neo4j-cypher "2.0.0"]]}}
  :main stubbly.core)
