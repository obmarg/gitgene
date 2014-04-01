(ns gitgene.routes
  (:use compojure.core
        gitgene.views
        [hiccup.middleware :only (wrap-base-url)])
  (:require [compojure.route :as route]
            [compojure.handler :as handler]
            [compojure.response :as response]
            [cemerick.shoreleave.rpc :refer (defremote wrap-rpc)]))

(defroutes main-routes
  (GET "/" [] (index-page))
  (route/resources "/")
  (route/not-found "Page not found"))

(defremote remote-fn [a] (* a 2))

(def app
  (-> main-routes
      wrap-rpc
      handler/site
      wrap-base-url))
