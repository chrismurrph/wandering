(ns app.api
  (:require [om.next.server :as om]
            [om.next.impl.parser :as op]
            [taoensso.timbre :as timbre]
            [endophile.core :as endo]
            [endophile.hiccup :as endohic]
            [hiccup.core :only [html]]
            [markdown.core :as md]
            ))

(defmulti apimutate om/dispatch)
(defmulti api-read om/dispatch)

(defmethod apimutate :default [e k p]
  (timbre/error "Unrecognized mutation " k))

(defmethod api-read :default [{:keys [ast query] :as env} dispatch-key params]
  (timbre/error "Unrecognized query " (op/ast->expr ast)))

(defmethod api-read :imported-plans [{:keys [ast query markdown] :as env} dispatch-key params]
  {:value [{:id 1 :markdown (:markdown-text markdown)}]})
