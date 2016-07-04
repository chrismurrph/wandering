(ns cljs.user
  (:require
    [app.core :refer [app]]
    [cljs.pprint :refer [pprint]]
    [devtools.core :as devtools]
    [untangled.client.impl.util :as util]
    [app.root :as root]
    [untangled.client.core :as uc]))

; Use Chrome...these enable proper formatting of cljs data structures!
(devtools/enable-feature! :sanity-hints)
(devtools/install!)

(def log-app-state (partial util/log-app-state app))

(comment "Just including root here s/be enough"
  (reset! app (uc/mount @app root/Root "app")))

