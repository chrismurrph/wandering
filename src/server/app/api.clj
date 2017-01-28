(ns app.api
  (:require [om.next.server :as om]
            [om.next.impl.parser :as op]
            [taoensso.timbre :as timbre]
            [hiccup.core :only [html]]
            [markdown.core :as md]
            ))

(defmulti apimutate om/dispatch)
(defmulti api-read om/dispatch)

(defmethod apimutate :default [_ k _]
  (timbre/error "Unrecognized mutation " k))

(defmethod api-read :default [{:keys [ast query]} _ _]
  (timbre/error "Unrecognized query " (op/ast->expr ast)))

(defmethod api-read :imported-docs [{:keys [filesystem]} _ _]
  {:value [(merge {:id 1} (select-keys filesystem [:regular-markdown :emails-included-markdown :signature :contacts :panel-height :animation?]))]})

(defmethod api-read :imported-logins [{:keys [filesystem]} _ _]
  {:value [(merge {:id 2 :app/authenticated? false :title "Default title" :only-ph-for-pw? false} (select-keys filesystem [:title :only-ph-for-pw?]))]})
