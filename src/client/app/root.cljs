(ns app.root
  (:require [om.dom :as dom]
            [om.next :as om :refer-macros [defui]]
            yahoo.intl-messageformat-with-locales
            [untangled.client.core :as uc]
            [app.core :as core]
            [app.ui :as ui]
            [app.login-dialog :as dialog]
            [app.utils :as u]
            [app.molecules :as moles]))

;;Might need
;;(enable-console-print!)

(defui ^:once Root
  static uc/InitialAppState
  (initial-state [clz params] {:app/docs [] :app/login-info []})
  static om/IQuery
  (query [this] [:ui/react-key
                 {:app/docs (om/get-query ui/ShowdownDocument)}
                 {:app/login-info (om/get-query dialog/LoginDialog)}])
  Object
  (cancel-sign-in-fn [this]
    (println "User cancelled, doing nothing, we ought to take user back to web page came from"))
  (sign-in-fn [this only-ph-for-pw? un pw]
    (println "Trying to sign in for: " un pw)
    (ui/login-process! this only-ph-for-pw? un pw (-> (om/props this) :app/docs first :contacts)))
  (render [this]
    (let [{:keys [ui/react-key app/docs app/login-info]} (om/props this)
          the-doc (first docs)
          ;_ (assert the-doc)
          the-login-info (first login-info)
          {:keys [app/authenticated?]} the-login-info
          ;_ (assert (u/boolean? authenticated?) (str "authenticated? should exist in the-login-info: " the-login-info))
          ]
      (dom/div #js{:key react-key}
               (if authenticated?
                 (ui/ui-showdown-document the-doc)
                 (dialog/ui-login-dialog (om/computed the-login-info {:sign-in-fn        #(.sign-in-fn this %1 %2 %3)
                                                                      :cancel-sign-in-fn #(.cancel-sign-in-fn this)})))))))
(reset! core/app (uc/mount @core/app Root "app"))
