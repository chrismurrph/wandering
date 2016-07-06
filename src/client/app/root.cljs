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

(defui ^:once Root
  static uc/InitialAppState
  (initial-state [clz params] {:plans [] :app/login-info (uc/initial-state dialog/LoginDialog {:app/name "SMARTGAS-connect marketing plan"})})
  static om/IQuery
  (query [this] [:ui/react-key
                 {:plans (om/get-query ui/ShowdownDocument)}
                 {:app/login-info (om/get-query dialog/LoginDialog)}])
  Object
  (cancel-sign-in-fn [this]
    (println "User cancelled, doing nothing, we ought to take user back to web page came from"))
  (sign-in-fn [this un pw]
    (println "Trying to sign in for: " un pw)
    (ui/login-process! this un pw (-> (om/props this) :plans first :contacts)))
  (render [this]
    (let [{:keys [ui/react-key plans app/login-info]} (om/props this)
          _ (assert login-info)
          {:keys [app/authenticated?]} login-info
          _ (assert (u/boolean? authenticated?) (str "authenticated? should exist in login-info: " login-info))
          the-plan (first plans)
          ]
      (dom/div #js{:key react-key}
               (if authenticated?
                 (ui/ui-showdown-document the-plan)
                 (dialog/ui-login-dialog (om/computed login-info {:sign-in-fn        #(.sign-in-fn this %1 %2)
                                                                  :cancel-sign-in-fn #(.cancel-sign-in-fn this)})))))))
(reset! core/app (uc/mount @core/app Root "app"))
