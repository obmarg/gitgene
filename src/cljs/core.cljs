(ns gitgene.core
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

(defn
  widget [data]
  (om/component
   (dom/div nil "Hello world")))

(om/root {} widget (.getElementById js/document "app"))