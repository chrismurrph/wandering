(ns app.ui
  (:require [om.dom :as dom]
            [om.next :as om :refer-macros [defui]]
            yahoo.intl-messageformat-with-locales
            [untangled.client.core :as uc]
            [untangled.client.mutations :as m]
            [reagent.core :as r]
            [app.molecules :as moles]
            [app.utils :as u]
            [cljs.core.async :as async
             :refer [<! >! chan close! put! timeout]])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(defui ^:once Item
  static uc/InitialAppState
  (initial-state [clz {:keys [id label]}] {:id id :label label})
  static om/IQuery
  (query [this] [:id :label])
  static om/Ident
  (ident [this {:keys [id]}] [:items/by-id id])
  Object
  (render [this]
    (let [{:keys [id label]} (om/props this)]
      (dom/li nil label))))
(def ui-item (om/factory Item {:keyfn :id}))

(defui Rect
  Object
  (render [this]
    (let [{:keys [x y]} (om/props this)
          rect-props {:x       x
                      :y       y
                      :width   30
                      :height  40
                      :opacity 0.1
                      ;:fill    fill
                      :rx      5 :ry 5}]
      (dom/rect (clj->js rect-props)))))
(def rect-comp (om/factory Rect {:keyfn :id}))

(defn accumulate-state [state]
  (let [res (as-> state $
                  (update $ :elapsed #(inc %))
                  (moles/emit-molecule-particles $)
                  (update $ :molecule-particles #(map moles/move-molecule-symbol %))
                  ;(u/probe "state" $)
                  )
        ;_ (moles/draw-state res)
        ]
    res))

(def test-molecules [{:id 1 :x 10 :y 10}
                     {:id 2 :x 20 :y 20}
                     {:id 3 :x 50 :y 50}
                     {:id 4 :x 60 :y 100}
                     {:id 5 :x 70 :y 220}
                     {:id 6 :x 80 :y 340}])

(defui ^once Molecules
  static om/IQuery
  (query [this] [:elapsed])
  Object
  (initLocalState [this]
    {:molecule-particles test-molecules})
  (componentDidMount [this]
    (let []
      (go-loop [state {:elapsed 0 :molecule-particles []}]
               (<! (timeout moles/wait-time))
               (when (< (:elapsed state) 3000)
                 (let [{:keys [molecule-particles elapsed] :as new-state} (accumulate-state state)]
                   (om/transact! this `[(app/elapsed {:elapsed ~elapsed})])
                   (when (moles/one-second-mark? elapsed)
                     (println "One sec mark")
                     (om/transact! this `[(app/bg-colour-change {:seconds-elapsed ~(/ elapsed moles/fps)}) [:plan/by-id 1]]))
                   (om/update-state! this assoc :molecule-particles molecule-particles)
                   ;(println "IN LOCAL STATE: " (count molecule-particles) "at" elapsed)
                   (recur new-state))))))
  (componentWillUnmount [this]
    (om/update-state! this dissoc :molecule-particles))
  (render [this]
    (let [particles (om/get-state this :molecule-particles)
          ;_ (println "In render with " (count particles))
          ]
      (dom/svg #js{:className "back" :height (str moles/height "px") :width (str moles/width "px")}
               (dom/g nil
                      (map rect-comp particles))))))
(def ui-molecules (om/factory Molecules {:keyfn :id}))

;;
;; A Javascript converter, so what has to come in is the Markdown itself
;;
(defui ^:once ShowdownPlan
  ;static uc/InitialAppState
  ;(initial-state [clz {:keys [id markdown]}] {:id id :markdown markdown})
  static om/IQuery
  (query [this] [:id :markdown :html-text {:elapsed-join (om/get-query Molecules)} :red :green :blue])
  static om/Ident
  (ident [this {:keys [id]}] [:plan/by-id id])
  Object
  (render [this]
    (let [{:keys [id html-text elapsed-join red green blue]} (om/props this)
          _ (println (str "r g b: ==" red "," green "," blue "=="))
          ;; Doesn't work, whereas: "rgba(200,200,200,0.3)" does!
          bg-colour (str "rgba(" red "," green "," blue ",0.3)")
          ;; This s/make bg transparent:
          ; background-color:rgba(255,0,0,0.5);
          ]
      (dom/div #js{:className "container"}
               (ui-molecules elapsed-join)
               (dom/div #js{:className "front" :style #js{:backgroundColor bg-colour}}
                        (dom/div #js{:className "inner-front"}
                                 (dom/div #js {:dangerouslySetInnerHTML #js {:__html html-text}} nil)))))))
(def ui-showdown-plan (om/factory ShowdownPlan {:keyfn :id}))

(defui ^:once Root
  static uc/InitialAppState
  (initial-state [clz params] {:plans []})
  static om/IQuery
  (query [this] [:ui/react-key {:plans (om/get-query ShowdownPlan)}])
  Object
  (render [this]
    (let [{:keys [ui/react-key plans]} (om/props this)
          _ (println "list in size: " (count (:markdown (first plans))))
          ]
      (dom/div #js{:key react-key}
               (dom/h4 nil "Header")
               (dom/div nil
                        (map ui-showdown-plan plans))))))
