(ns gitgene.views
  (:require
    [hiccup
      [page :refer [html5]]
      [page :refer [include-js]]]))

(defn index-page []
  (html5
    [:head
     [:title "Hello World"]]
    [:body
     [:h1 "Hello World"]
     [:div {:id "app"}]
     (include-js
       "http://fb.me/react-0.8.0.js"
       "out/goog/base.js"
       "/js/main.js")
     [:script "goog.require('gitgene.core');"]]))