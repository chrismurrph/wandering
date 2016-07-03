(ns app.ui
  (:require [om.dom :as dom]
            [om.next :as om :refer-macros [defui]]
            yahoo.intl-messageformat-with-locales
            [untangled.client.core :as uc]
            [app.molecules :as moles]
            [clojure.string :as str]
            [cljs.core.async
             :refer [<! >! chan close! put! timeout]])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

;
; plain React components.
;
(def side-length 50)

(defui Molecule
  Object
  (render [this]
    (let [{:keys [x y mole-fill last-degrees-angle symbol-txt max-saturation]} (om/props this)
          [r g b] mole-fill
          saturation (moles/calc-distance-saturation {:x x :y y :max-saturation max-saturation})
          colour (str "rgba(" r "," g "," b "," saturation ")")
          centre-x (+ x (/ side-length 2))
          centre-y (+ y (/ side-length 2))
          svg-props {:x           x
                     :y           y
                     :stroke      colour
                     :fill        colour
                     :stroke-with 10
                     ;; Without x and y they don't start off in the hatchery area. i.e. x and y not respected
                     :transform   (str "rotate(" last-degrees-angle "," centre-x "," centre-y ")")
                     :dx          side-length
                     :dy          side-length
                     :font-family "Verdana"
                     :font-size   55
                     }]
      (dom/text (clj->js svg-props) symbol-txt))))
(def molecule-comp (om/factory Molecule {:keyfn :id}))

;
; Use for debugging because it is easier to see than the Molecule
;
(defui Rect
  Object
  (render [this]
    (let [{:keys [id x y mole-fill last-degrees-angle max-saturation]} (om/props this)
          saturation (moles/calc-distance-saturation {:x x :y y :max-saturation max-saturation})
          centre-x (+ x (/ side-length 2))
          centre-y (+ y (/ side-length 2))
          _ (when (= (str id) "G__1")
              (println "Sat: " id saturation))
          [r g b] mole-fill
          rect-props {:x         x
                      :y         y
                      :width     side-length
                      :height    side-length
                      :opacity   0.2
                      ;; Harder to see than not having
                      :fill      (str "rgba(" r "," g "," b "," saturation ")")
                      ;; Without x and y they don't start off in the hatchery area. i.e. x and y not respected
                      :transform (str "rotate(" last-degrees-angle "," centre-x "," centre-y ")")
                      :rx        5
                      :ry        5}]
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
                   (when (moles/ten-second-mark? elapsed)
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
      (dom/svg #js{:className "back"
                   :opacity   "0.85"
                   :height    (str moles/height "px")
                   :width     (str moles/width "px")}
               (dom/g nil
                      (map molecule-comp particles))))))
(def ui-molecules (om/factory Molecules {:keyfn :id}))

(defn centre-first-heading [html]
  (when html
    (let [extra " class=title "
          tag "<h2 "
          idx (.indexOf html tag)
          pos (count tag)
          start-sub (subs html idx pos)
          end-sub (subs html pos)
          ;_ (println start-sub)
          ;_ (println end-sub)
          ]
      (str start-sub extra end-sub))))

(defui ^:once Signature
  static om/IQuery
  (query [this] [:name :phone :email])
  Object
  (render [this]
    (let [{:keys [name phone email]} (om/props this)]
      ;;
      ;; I would have thought this would go to the middle. But on the left
      ;; will do for now
      ;;
      (dom/div #js{:className "tubes-general-container"}
               (dom/div nil (str "     " name))
               (dom/div #js{:className "side fa fa-phone fa-1x"} (str "     " phone))
               (dom/div "")
               (dom/div #js{:className "fa fa-envelope fa-1x"} (str "     " email))))))
(def ui-signature (om/factory Signature))

(defui ^:once BackgroundColour
  static om/IQuery
  (query [this] [:red :green :blue]))

(defui ^:once ShowdownPlan
  static om/IQuery
  (query [this] [:id :markdown :markup
                 {:signature (om/get-query Signature)}
                 {:elapsed-join (om/get-query Molecules)}
                 {:bg-colour (om/get-query BackgroundColour)}])
  static om/Ident
  (ident [this {:keys [id]}] [:plan/by-id id])
  Object
  (render [this]
    (let [{:keys [id markup signature elapsed-join bg-colour]} (om/props this)
          {:keys [red green blue]} bg-colour
          red (or red (moles/red-pulse 1))
          green (or green (moles/green-pulse 1))
          blue (or blue (moles/blue-pulse 1))
          _ (println (str "r g b: ==" red "," green "," blue "=="))
          background-fill (str "rgba(" red "," green "," blue ",0.3)")
          titled-markup (centre-first-heading markup)
          ]
      (dom/div #js{:className "container" :style #js{:width  (str moles/width "px")
                                                     :height (str moles/height "px")}}
               (ui-molecules elapsed-join)
               (dom/div #js{:className "front" :style #js{:backgroundColor background-fill}}
                        (dom/div #js{:className "inner-front"}
                                 (dom/div #js {:dangerouslySetInnerHTML #js {:__html titled-markup}} nil)
                                 (ui-signature signature)))))))
(def ui-showdown-plan (om/factory ShowdownPlan {:keyfn :id}))

(defui ^:once Root
  static om/IQuery
  (query [this] [:ui/react-key {:plans (om/get-query ShowdownPlan)}])
  Object
  (render [this]
    (let [{:keys [ui/react-key plans]} (om/props this)
          the-plan (first plans)
          ]
      (dom/div #js{:key react-key}
               (dom/div nil
                        (ui-showdown-plan the-plan))))))
