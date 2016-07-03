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

(defmethod api-read :imported-plans [{:keys [filesystem]} _ _]
  {:value [{:id 1
            :markdown (:markdown-text filesystem)
            :signature (:signature filesystem)
            :contacts (:contacts filesystem)
            }
           ]})
