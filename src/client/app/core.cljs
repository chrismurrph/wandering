(ns app.core
  (:require
    app.mutations
    cljsjs.showdown
    [untangled.client.data-fetch :as df]
    [untangled.client.core :as uc]
    [untangled.client.impl.network :as net]
    [om.next :as om]
    [app.ui :as ui]
    [app.login-dialog :as ld]))

;; Either "marketing" or "uneasy" for prod build, and uncomment networking below
;; When developing simply go in as http://localhost:3000/ from browser
;; For server side just need to run the right script
(def app-name "uneasy")
(def specific-url (str app-name "/api"))

(defonce app (atom (uc/new-untangled-client
                     ;:networking (net/make-untangled-network specific-url :global-error-callback (constantly nil))
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

