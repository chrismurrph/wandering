(ns app.core
  (:require
    app.mutations
    cljsjs.showdown
    [untangled.client.data-fetch :as df]
    [untangled.client.core :as uc]
    [om.next :as om]
    [app.ui :as ui]))

(defonce app (atom (uc/new-untangled-client
                     :started-callback
                     (fn [{:keys [reconciler]}]
                       (df/load-data reconciler [{:imported-plans (om/get-query ui/ShowdownDocument)}]
                                     :post-mutation 'fetch/plan-loaded
                                     :refresh [:plans])))))

(defn my-reconciler-available? []
  (:reconciler @app))

(defn my-reconciler []
  (let [rec (:reconciler @app)
        _ (assert rec "No reconciler available")]
    rec))

