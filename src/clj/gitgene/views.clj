(ns gitgene.views
  (:require
    [hiccup
      [page :refer [html5]]
      [page :refer [include-js include-css]]]))

(defn menu-item
  [text icon link]
  [:li  [:a {:href link} [:i {:class icon}] " " text]])

(defn active-menu-item
  [text icon link]
  [:li {:class "active"} [:a {:href link} [:i {:class icon}] " " text]])

(defn index-page []
  (html5
    [:head
     [:meta {:charset "utf-8"}]
     [:meta {:http-equiv "X-UA-Compatible" :content "IE=edge"}]
     [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
     [:title "Hello World"]
     (include-css
      "css/bootstrap.min.css"
      "//netdna.bootstrapcdn.com/font-awesome/4.0.3/css/font-awesome.css")]
    [:body
     [:nav {:class "navbar navbar-inverse navbar-fixed-top" :role "navigation"}
      [:div {:class "container-fluid"}
       [:div {:class "navbar-header"}
        [:a {:class "navbar-brand" :href "#"}
         [:i {:class "fa fa-flask"}] " GitGene"]]]]
     [:div {:class "container-fluid" :style "margin-top: 60px"}
      [:div {:class "row"}
       [:div {:class "col-xs-9 col-xs-offset-3"}
        [:ul {:class "nav nav-pills"}
         (active-menu-item "By Person" "fa fa-user" "#")
         (menu-item "In A File" "fa fa-file" "#")
         (menu-item "In A Commit" "fa fa-files-o" "#")
         ]]]
     [:div {:class "row"}
      [:div {:class "col-xs-3"}
       [:ul {:class "nav nav-pills nav-stacked"}
        (active-menu-item "Number Of Changes" "fa fa-exchange" "#")
        (menu-item "Number Of Additions" "fa fa-plus" "#")
        (menu-item "Number Of Deletions" "fa fa-minus" "#")
        (menu-item "Total Churn" "fa fa-refresh" "#")
        ]]
      [:div {:class "col-xs-9"}
       [:div {:id "app" :style "padding-top: 15px"}]]]]]
     (include-js
       "http://fb.me/react-0.8.0.js"
       "out/goog/base.js"
       "/js/main.js")
     [:script "goog.require('gitgene.core');"]))
