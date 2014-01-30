(ns gitgene.core
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

(defn
  widget [data]
  (om/component
   (dom/div nil "Hello world")))

(defn
  table-row [columns]
  (om/component
   (apply dom/tr nil
    (map #(dom/td nil %) columns))))

(defn
  table [data]
  (om/component
   (dom/table #js {:className "table table-striped"}
     (dom/thead nil
      (apply dom/tr nil
       (map #(dom/th nil %) (:headers data))))
     (apply dom/tbody nil
      (om/build-all table-row (:rows data))))))

(def app-state (atom {:headers ["test" "test 2"]
                      :rows [["1" "2"] ["3" "4"]]}))

(om/root app-state table (.getElementById js/document "app"))
