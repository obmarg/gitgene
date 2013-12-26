(ns stubbly.db-structure-test
  (:use midje.sweet
        [clojure.walk :only [keywordize-keys]])
  (:import [org.neo4j.test TestGraphDatabaseFactory]
           [org.neo4j.cypher.javacompat ExecutionEngine]))

(declare create-database)

(def test-data
  "
  CREATE
  (graeme:User {name:'Graeme'}),
  (james:User {name:'James'}),

  (config:File {name: 'config.py',
                path: 'path/to/config.py',
                type: 'python'}),
  (routes:File {name: 'routes.py',
                path: 'another/path/routes.py',
                type: 'python'}),

  (g_commit1:Commit {date: 'sometime'}),
  (g_commit2:Commit {date: 'some-other-time'}),
  (g_commit3:Commit {date: 'blah'}),

  (j_commit1:Commit {date: 'sometime'}),
  (j_commit2:Commit {date: 'some-other-time'}),

  (config_l1:Line {contents: 'import os'}),
  (config_l2:Line {contents: 'config = SOMETHING'}),
  (config_l3:Line {contents: 'config = SOMETHINGELSE'}),

  (routes_l1:Line {contents: 'import config'}),
  (routes_l2:Line {contents: 'import flask'}),
  (routes_l3:Line {contents: 'app = flask()'}),
  (routes_l4:Line {contents: '@app.route(a_route, type=POST)'}),
  (routes_l5:Line {contents: 'def a_route(params):'}),
  (routes_l6:Line {contents: '    return hello world'}),
  (routes_l7:Line {contents: '    return something else'}),

  g_commit1-[:AUTHOR]->graeme,
  g_commit2-[:AUTHOR]->graeme,
  g_commit3-[:AUTHOR]->graeme,
  j_commit1-[:AUTHOR]->james,
  j_commit2-[:AUTHOR]->james,

  config-[:CONTAINS]->config_l1,
  config-[:CONTAINS]->config_l2,
  config-[:CONTAINS]->config_l3,
  routes-[:CONTAINS]->routes_l1,
  routes-[:CONTAINS]->routes_l2,
  routes-[:CONTAINS]->routes_l3,
  routes-[:CONTAINS]->routes_l4,
  routes-[:CONTAINS]->routes_l5,
  routes-[:CONTAINS]->routes_l6,
  routes-[:CONTAINS]->routes_l7,

  g_commit1-[:ADDED_LINE]->config_l1,
  g_commit2-[:ADDED_LINE]->config_l2,
  g_commit3-[:ADDED_LINE]->config_l3,
  g_commit3-[:ADDED_LINE]->routes_l1,
  g_commit3-[:ADDED_LINE]->routes_l2,
  g_commit3-[:ADDED_LINE]->routes_l3,

  j_commit1-[:ADDED_LINE]->routes_l4,
  j_commit1-[:ADDED_LINE]->routes_l5,
  j_commit1-[:ADDED_LINE]->routes_l6,
  j_commit2-[:ADDED_LINE]->routes_l7
  ")

(def test-query (atom nil))

; TODO: Ideally I need to source these queries from somewhere.
;       so I'm testing the actual application queries in here...

(with-state-changes [(before :facts (reset! test-query
                                            (partial execute-query
                                                     (create-database test-data))))]
  (fact "We should be able to find how many lines each user has added"
        (@test-query "MATCH c-[:ADDED_LINE]->l, c-[:AUTHOR]->a
                     RETURN a.name AS name, COUNT(l) AS num")
        => (just #{{"name" "Graeme"
                    "num" 6}
                   {"name" "James"
                    "num" 4}})))

(defn- execute-query
  "Executes a cypher query and returns the results.
   This doesnt use named params, so should never be used in prod"
  [db cypher]
  (let [engine (ExecutionEngine. db)
        result (.execute engine cypher)]
    (sequence result)))

(defn- create-database
  "Creates and returns database using the cypher passed in"
  [cypher]
  (let [db (.newImpermanentDatabase (TestGraphDatabaseFactory. ))
        engine (ExecutionEngine. db)]
    (.execute engine cypher)
    db))
