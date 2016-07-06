(ns app.main
  (:require [app.core :as core]
            [untangled.client.core :as uc]
            [app.root :as root]))

(enable-console-print!)

(defonce mounted-app (reset! core/app (uc/mount @core/app root/Root "app")))
