(ns app.core
  (:require
    app.mutations
    cljsjs.showdown
    [untangled.client.data-fetch :as df]
    [untangled.client.core :as uc]
    [om.next :as om]
    [app.ui :as ui]
    [app.login-dialog :as ld]))

(defonce app (atom (uc/new-untangled-client
                     :started-callback
                     (fn [{:keys [reconciler]}]
                       (df/load-data reconciler [{:imported-docs (om/get-query ui/ShowdownDocument)}
                                                 {:imported-logins (om/get-query ld/LoginDialog)}]
                                     :post-mutation 'fetch/init-state-loaded
                                     :refresh [:app/docs :app/login-info])))))

(defn my-reconciler-available? []
  (:reconciler @app))

(defn my-reconciler []
  (let [rec (:reconciler @app)
        _ (assert rec "No reconciler available")]
    rec))

