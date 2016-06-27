(ns app.mutations
  (:require [untangled.client.mutations :as m]
            [untangled.client.core :as uc]
            [app.ui :as ui]
            [om.next :as om]))

(defmethod m/mutate 'app/add-item [{:keys [state ref]} k {:keys [id label]}]
  {:action (fn []
             (let [list-path (conj ref :items)
                   new-item (uc/initial-state ui/Item {:id id :label label})
                   item-ident (om/ident ui/Item new-item)]
               ; place the item in the db table of items
               (swap! state assoc-in item-ident new-item)
               ; tack on the ident of the item in the list
               (uc/integrate-ident! state item-ident :append list-path)))})

;;
;; Need to adjust state so that what has been placed in :all-items is transfered
;; into :items in "Initial List"
;;
(defmethod m/mutate 'fetch/items-loaded
  [{:keys [state]} _ _]
  {:action (fn []
             (let [idents (get @state :all-items)
                   target-ref [:lists/by-title "Initial List" :items]
                   _ (println "Loading items post mutation targetting " idents)]
               (swap! state (fn [st]
                              (-> st
                                  (assoc-in target-ref idents)
                                  (dissoc :all-items))))))})

(defmethod m/mutate 'fetch/plan-loaded
  [{:keys [state]} _ _]
  {:action (fn []
             (let [idents (get @state :imported-plans)
                   _ (println "Loading items post mutation targetting " idents)]
               ;state
               (swap! state (fn [st]
                              (-> st
                                  (assoc :plans idents)
                                  (assoc :imported-plans []))))))})
