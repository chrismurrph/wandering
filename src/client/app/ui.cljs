(ns app.ui
  (:require [om.dom :as dom]
            [om.next :as om :refer-macros [defui]]
            yahoo.intl-messageformat-with-locales
            [untangled.client.core :as uc]
            [untangled.client.mutations :as m]
            [reagent.core :as r]
            [cljsjs.showdown :as showdown]
            [app.molecules :as moles]
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

(defn convert-to-html [markdown]
  ;; note the syntax below: js/VarFromExternsFile.property
  ;; the dot on the end is the usual Clojure interop syntax: (Constructor. constructor-arg constructor-arg)
  ;; #js {:tables true}
  (let [converter (js/Showdown.converter.)
        ;_ converter.setOption('optionKey', 'value');
        ;; Apparently this function doesn't even exist
        ;_ (.setOption converter "tables" true)
        ]
    ;; methods you call will generally need to be called out as prototype values in the externs
    (.makeHtml converter markdown)))

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

(defn update-state [state]
  (let [res (-> state
            (update :elapsed #(inc %))
            moles/emit-molecule-particles
            (update :molecule-particles (fn [molecule-particles] (map moles/move-molecule-symbol molecule-particles))))
        _ (moles/draw-state res)]
    res))

(def test-molecules [{:id 1 :x 10 :y 10}
                     {:id 2 :x 20 :y 20}
                     {:id 3 :x 50 :y 50}
                     {:id 4 :x 60 :y 100}
                     {:id 5 :x 70 :y 220}
                     {:id 6 :x 80 :y 340}])

(defui ^once Molecules
  Object
  (initLocalState [this]
    {:molecules test-molecules})
  (componentDidMount [this]
    (let []
      (go-loop [state {:molecule-particles [] :elapsed 0}]
               (<! (timeout moles/wait-time))
               ;(println "11 times a sec?")
               (let [new-state (update-state state)]
                 (recur new-state)))))
  (render [this]
    (let []
      (dom/svg #js{:className "back" :height "3000px"}
               (map rect-comp (om/get-state this :molecules))))))
(def ui-molecules (om/factory Molecules {:keyfn :id}))

;;
;; A Javascript converter, so what has to come in is the Markdown itself
;;
(defui ^:once ShowdownPlan
  static uc/InitialAppState
  (initial-state [clz {:keys [id markdown]}] {:id id :markdown markdown})
  static om/IQuery
  (query [this] [:id :markdown])
  static om/Ident
  (ident [this {:keys [id]}] [:plan/by-id id])
  Object
  (render [this]
    (let [{:keys [id markdown]} (om/props this)
          _ (println (str "MD size: " (count markdown)))
          ;; This s/make bg transparent:
          ; background-color:rgba(255,0,0,0.5);
          ;; Need to convert here:
          text (convert-to-html markdown)
          ;_ (println (str "HTML: " text))
          ]
      (dom/div #js{:className "container"}
               (ui-molecules)
               (dom/div #js{:className "front" :style #js{:backgroundColor "rgba(255,255,255,0.3)"}}
                        (dom/div #js {:dangerouslySetInnerHTML #js {:__html text}} nil))))))
(def ui-showdown-plan (om/factory ShowdownPlan {:keyfn :id}))

(defui ^:once MyList
  static uc/InitialAppState
  (initial-state [clz params] {:title             "Initial List"
                               :ui/new-item-label ""
                               :items             []})
  static om/IQuery
  (query [this] [:ui/new-item-label :title {:items (om/get-query Item)}])
  static om/Ident
  (ident [this {:keys [title]}] [:lists/by-title title])
  Object
  (render [this]
    (let [{:keys [title items ui/new-item-label] :or {ui/new-item-label ""}} (om/props this)
          _ (println "Count items: " (count items))]
      (dom/div nil
        (dom/h4 nil title)
        (dom/input #js {:value    new-item-label
                        :onChange (fn [evt] (m/set-string! this :ui/new-item-label :event evt))})
        (dom/button #js {:onClick #(om/transact! this `[(app/add-item {:label ~new-item-label})])} "+")
        (dom/ul nil
          (map ui-item items))))))
(def ui-list (om/factory MyList))

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
