(ns gitgene.db-structure-test
  (:use midje.sweet
        [clojure.walk :only [keywordize-keys]])
  (:import [org.neo4j.test TestGraphDatabaseFactory]
           [org.neo4j.cypher.javacompat ExecutionEngine]))

(declare create-database execute-query)

; TODO: Going to need to add support for multiple projects
;       to this db structure.
; TODO: Could make dates into a graph for nice querying of them.
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

  (config_l1:Line {contents: 'import os', linenum: 1}),
  (config_l2:Line {contents: 'config = SOMETHING', linenum: 2}),
  (config_l3:Line {contents: 'config = SOMETHINGELSE', linenum: 2}),

  (routes_l1:Line {contents: 'import config', linenum: 1}),
  (routes_l2:Line {contents: 'import flask', linenum: 2}),
  (routes_l3:Line {contents: 'app = flask()', linenum:3 }),
  (routes_l4:Line {contents: '@app.route(a_route, type=POST)', linenum: 4}),
  (routes_l5:Line {contents: 'def a_route(params):', linenum: 5}),
  (routes_l6:Line {contents: '    return hello world', linenum: 6}),
  (routes_l7:Line {contents: '    return something else', linenum: 6}),

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

  g_commit3-[:REMOVED_LINE]->config_l2,
  g_commit3-[:ADDED_LINE]->config_l3,
  g_commit3-[:ADDED_LINE]->routes_l1,
  g_commit3-[:ADDED_LINE]->routes_l2,
  g_commit3-[:ADDED_LINE]->routes_l3,

  j_commit1-[:ADDED_LINE]->routes_l4,
  j_commit1-[:ADDED_LINE]->routes_l5,
  j_commit1-[:ADDED_LINE]->routes_l6,

  j_commit2-[:REMOVED_LINE]->routes_l6,
  j_commit2-[:ADDED_LINE]->routes_l7,
  j_commit2-[:REMOVED_LINE]->routes_l1
  ")


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
                    "num" 4}}))

  (fact "We should be able to find how many lines a particular user has added"
        (@test-query "MATCH c-[:ADDED_LINE]->l, c-[:AUTHOR]->a
                      WHERE a.name = 'Graeme'
                      RETURN a.name AS name, COUNT(l) AS num")
        => [{"name" "Graeme"
             "num" 6}])

  (fact "We should be able to find how many lines each user has removed"
        (@test-query "MATCH c-[:REMOVED_LINE]->l, c-[:AUTHOR]->a
                      RETURN a.name AS name, COUNT(l) AS num")
        => (just #{{"name" "Graeme"
                    "num" 1}
                   {"name" "James"
                    "num" 2}}))

  (fact "We should be able to find how many lines a particular user has removed"
        (@test-query "MATCH c-[:REMOVED_LINE]->l, c-[:AUTHOR]->a
                      WHERE a.name = 'Graeme'
                      RETURN a.name AS name, COUNT(l) AS num")
        => [{"name" "Graeme"
             "num" 1}])

  (fact "We should be able to see how many files people touched"
        (@test-query "MATCH c-[:ADDED_LINE|:REMOVED_LINE]->l<-[:CONTAINS]-f,
                            c-[:AUTHOR]->a
                      RETURN a.name AS name, COUNT(DISTINCT f) AS num")
        => (just #{{"name" "Graeme"
                    "num" 2}
                   {"name" "James"
                    "num" 1}}))

  (fact "We should be able to see what files are changed the most"
        (@test-query "MATCH c-[:ADDED_LINE|:REMOVED_LINE]->l<-[:CONTAINS]-f
                      RETURN f.name AS name, COUNT(DISTINCT l) AS num")
        => (just #{{"name" "config.py"
                    "num" 3}
                   {"name" "routes.py"
                    "num" 7}}))

  (fact "We should be able to see who changed how many lines per file"
        (@test-query "MATCH c-[:ADDED_LINE|:REMOVED_LINE]->l<-[:CONTAINS]-f,
                            c-[:AUTHOR]->a
                      RETURN a.name AS author, f.name AS file,
                             COUNT(DISTINCT l) AS num")
        => (just #{{"author" "Graeme"
                    "file" "routes.py"
                    "num" 3}
                   {"author" "Graeme"
                    "file" "config.py"
                    "num" 3}
                   {"author" "James"
                    "file" "routes.py"
                    "num" 5}}))

  (fact "We should be able to see who's code is removed the most"
        (@test-query "MATCH c-[:REMOVED_LINE]->l<-[:ADDED_LINE]-c2-[:AUTHOR]->a
                      RETURN a.name AS name, COUNT(l) AS num")
        => (just #{{"name" "Graeme"
                    "num" 2}
                   {"name" "James"
                    "num" 1}}))

  (fact "We should be able to see who replaces other peoples code"
        (@test-query "MATCH c-[:REMOVED_LINE]->l<-[:ADDED_LINE]-c2-[:AUTHOR]->add_a,
                            c-[:AUTHOR]->rem_a
                      WHERE rem_a <> add_a
                      RETURN add_a.name AS author, rem_a.name AS remover,
                             COUNT(DISTINCT l) AS num")
        => (just #{{"author" "Graeme"
                    "remover" "James"
                    "num" 1}}))

  (fact "How many lines in current code base"
        (@test-query "MATCH (l:Line)
                      WHERE not(l<-[:REMOVED_LINE]-())
                      RETURN COUNT(DISTINCT l) AS count")
        => (just [{"count" 7}])))
