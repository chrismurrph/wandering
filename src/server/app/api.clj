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

(def plans (atom [{:id 1 :text "My Plan"}]))

(defmethod apimutate :default [e k p]
  (timbre/error "Unrecognized mutation " k))

(defmethod api-read :default [{:keys [ast query] :as env} dispatch-key params]
  (timbre/error "Unrecognized query " (op/ast->expr ast)))

(def marketing-plan (atom [:p "Marketing plan"]))

(def items (atom [{:id 1 :text "Sure yes will"}]))

;[endophile.core :only [mp to-clj html-string]]
;[endophile.hiccup :only [to-hiccup]]
;[hiccup.core :only [html]]
(defn read-into-marketing-plan-1 []
  (let [parsed (slurp "README.md")
        hiccup (into [:div] (endohic/to-hiccup parsed))]
    hiccup))

(defn read-into-marketing-plan []
  (let [md-str-in (slurp "marketing_for_kevin")
        html (md/md-to-html-string md-str-in :reference-links? true)]
    html))

(defn read-raw-plan []
  (let [md-str-in (slurp "marketing_for_kevin")]
    md-str-in))

(defmethod api-read :imported-plans [{:keys [ast query] :as env} dispatch-key params]
  (let [raw-plan (read-raw-plan)
        ;_ (println html-plan)
        ]
    {:value [{:id 1 :markdown raw-plan}]}))
