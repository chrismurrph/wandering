(ns app.ui
  (:require [om.dom :as dom]
            [om.next :as om :refer-macros [defui]]
            yahoo.intl-messageformat-with-locales
            [untangled.client.core :as uc]
            [app.molecules :as moles]
            [cljs.core.async
             :refer [<! >! chan close! put! timeout]])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(defui Rect
  Object
  (render [this]
    (let [{:keys [x y mole-fill degrees-angle]} (om/props this)
          [r g b] mole-fill
          rect-props {:x       x
                      :y       y
                      :width   30
                      :height  40
                      :opacity 0.1
                      ;; Harder to see than not having
                      :fill    (str "rgb(" r "," g "," b ")")
                      ;; Without x and y they don't start off in the hatchery area. i.e. x and y not respected
                      :transform (str "rotate(" degrees-angle "," x "," y ")")
                      :rx      5
                      :ry      5}]
      (dom/rect (clj->js rect-props)))))
(def rect-comp (om/factory Rect {:keyfn :id}))

(defn emit-move-molecules [state]
  (as-> state $
        (update $ :elapsed #(inc %))
        (moles/emit-molecule-particles $)
        (update $ :molecule-particles #(map moles/move-molecule-symbol %))
        ;(u/probe "state" $)
        ))

(defui ^once Molecules
  static om/IQuery
  (query [this] [:elapsed])
  Object
  (componentDidMount [this]
    (let []
      (go-loop [state {:elapsed 0 :molecule-particles []}]
               (<! (timeout moles/wait-time))
               (when (< (:elapsed state) 3000)
                 (let [{:keys [molecule-particles elapsed] :as new-state} (emit-move-molecules state)]
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

(defui ^:once ShowdownPlan
  static om/IQuery
  (query [this] [:id :markdown :html-text {:elapsed-join (om/get-query Molecules)} :red :green :blue])
  static om/Ident
  (ident [this {:keys [id]}] [:plan/by-id id])
  Object
  (render [this]
    (let [{:keys [id html-text elapsed-join red green blue]} (om/props this)
          red (or red (moles/red-pulse 1))
          green (or green (moles/green-pulse 1))
          blue (or blue (moles/blue-pulse 1))
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
