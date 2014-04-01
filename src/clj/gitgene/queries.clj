(ns gitgene.queries
  #_(:require))

; Essentially, we have 2 variables:
;   Line type: All, Added, Removed, Churn.
;              This affects what kind of line -> commit relationship we want.
;              Added, Removed and All are pretty obvious.
;              Churn essentially means we want lines that were added but then removed.
;
;   Result Type:  By Person, In a File, In A Commit:
;                 By Person, we need to return the persons name, and the unique line count.
;                 In a File, we need to return the file name and the unique line count.
;                 In a Commit, we need to return the commit & the unique line count?
;
;                 File & Commit don't sound that good, so maybe those should actually be modifiers
;                 on the existing Person option - by person by file, and by person by commit?
;                 Though in some cases we'd want to see files changed overall, in others
;                 files changed per person...
;                 Might at least need to rethink the UI for this...

;                 On top of that, not sure by commit even makes sense...
;
;                 All, Added & Removed are pretty simple.
;                 Churn may require an extra bit of work (to optionally ensure the users are
;                                                         not the same).
;

(declare build-match build-return)

(defn build-query
  [{:keys [line-type result-type]}]
  (apply str
    ((juxt build-match build-return) line-type result-type)))

(print (build-query
        {:line-type :added
         :result-type :people}))

(defn- line-type->relationship
  [line-type]
  (case line-type
    (:all :churn) ":ADDED_LINE|:REMOVED_LINE"
    :added ":ADDED_LINE"
    :removed ":REMOVED_LINE"))

(defn- result-type->author-clause
  [result-type]
  (case result-type
    :file "<-[:CONTAINS]-f"
    ""))

(defn- build-match
  "Builds the match clause of the cypher query"
  [line-type result-type]
  (let [relationship (line-type->relationship line-type)
        author-clause (result-type->author-clause result-type)]
    (str "MATCH c-[" relationship "]->l" author-clause ",\n\t"
         "c-[:AUTHOR]->a" "\n")))

(defn- build-return
  "Builds the return clause of the cypher query"
  [line-type result-type]
  ; TODO: Make this actually flexible.  At present it's a bit hard-coded...
  ;       In particular really doesn't work for churn.
  (str "RETURN a.name AS name, COUNT DISTINCT(l) AS num"))
