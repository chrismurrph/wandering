(ns app.login-dialog
  (:require [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [app.utils :as u]
            [untangled.client.core :as uc]))

(defui LoginDialog
  ;static uc/InitialAppState
  ;(initial-state [clz params] {:id 2 :app/name (:app/name params) :app/authenticated? false :title "Default Title"})
  static om/Ident
  (ident [this props]
    [:login-dlg/by-id (:id props)])
  static om/IQuery
  (query [this]
    [:id
     :app/authenticated?
     :title])
  Object
  (initLocalState [this]
    {:un "" :pw ""})
  (sign-in [this un pw]
    (let [{:keys [sign-in-fn]} (om/get-computed this)]
      ;(println "In sign in with " (om/get-computed this))
      (assert sign-in-fn)
      (sign-in-fn un pw)))
  (render [this]
    (let [{:keys [id app/authenticated? title]} (om/props this)
          ;_ (assert id (str "No id where props: " (om/props this)))
          ;_ (assert (u/boolean? authenticated?))
          ;_ (assert title (str "No title in LoginDialog: " (dissoc (om/props this) :om.next/computed)))
          {:keys [cancel-sign-in-fn]} (om/get-computed this)
          un (om/get-state this :un)
          pw (om/get-state this :pw)
          _ (assert un)
          _ (assert pw)]
      (dom/div #js {:className "dialog"}
               (dom/div #js {:className "dialog-closer" :onClick cancel-sign-in-fn})
               (dom/div #js {:className "dialog-content"}
                        (dom/h1 #js {:className "dialog-title"}
                                "" (dom/span #js {:className "board-name"} title))
                        (dom/form #js {:onSubmit #(.preventDefault %)}
                                  (dom/div #js {:className "form-row"}
                                           (dom/label nil "Name:")
                                           (dom/input
                                             #js {:value       un
                                                  :placeholder "Enter user name here..."
                                                  :onChange    #(om/update-state! this assoc :un (.. % -target -value))}))
                                  (dom/div #js {:className "form-row"}
                                           (dom/label nil "Password:")
                                           (dom/input
                                             #js {:value       pw
                                                  :placeholder "Enter user password here..."
                                                  :onChange    #(om/update-state! this assoc :pw (.. % -target -value))})))
                        (dom/p #js {:className "dialog-buttons"}
                               (dom/button #js{:onClick #(.sign-in this un pw)} "Sign in")
                               #_(dom/button #js{:onClick cancel-sign-in-fn} "Cancel")))))))

(def ui-login-dialog (om/factory LoginDialog {:keyfn :id}))
