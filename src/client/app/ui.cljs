(ns app.ui
  (:require [om.dom :as dom]
            [om.next :as om :refer-macros [defui]]
            yahoo.intl-messageformat-with-locales
            [app.molecules :as moles]
            [app.utils :as u]
            [cljs.core.async
             :refer [<! >! chan close! put! timeout]]
            [clojure.string :as str]
            [cljs-time.core :as time]
            [cljs-time.format :as format-time]
            )
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

;
; plain React components.
;
(def side-length 50)

(defui ^:once Molecule
  Object
  (render [this]
    (let [{:keys [panel-height x y mole-fill last-degrees-angle symbol-txt max-saturation]} (om/props this)
          _ (assert panel-height)
          [r g b] mole-fill
          saturation (moles/calc-distance-saturation panel-height {:x x :y y :max-saturation max-saturation})
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
(defui ^:once Rect
  Object
  (render [this]
    (let [{:keys [id panel-height x y mole-fill last-degrees-angle max-saturation]} (om/props this)
          saturation (moles/calc-distance-saturation panel-height {:x x :y y :max-saturation max-saturation})
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

;;
;; Things are happening. Even if we have stopped creating new molecules, we are at
;; least moving them further away
;;
(defn emit-and-move-molecules [panel-height state]
  (u/log-off (str "Emitting: " (select-keys state [:elapsed])))
  (-> state
      (update :elapsed #(inc %))
      (moles/emit-molecule-particles panel-height)
      (update :molecule-particles #(map moles/move-molecule-symbol %))
      ))

(def date-time-formatter (format-time/formatter "dd_MM_yyyy__HH_mm_ss.SSS"))

;;
;; Start of quiet period
;;
(defn start-over [state]
  (u/log-on (str "Starting over at " (as-> (time/now) $
                                           (time/to-default-time-zone $)
                                           (format-time/unparse date-time-formatter $))))
  (-> state
      (assoc :elapsed -9000)
      (assoc :molecule-particles [])
      ))

;;
;; Quiet period, waiting for next chance to emit again. Too annoying to have them all the time!
;; :molecule-particles is an empty vector and staying that way
;;
(defn mark-time [state]
  (update state :elapsed #(inc %)))

(defn to-next-state [component panel-height colour-change-fn state]
  (let [elapsed (:elapsed state)
        _ (u/log-off (str "elapsed: " elapsed))]
    (cond
      (< elapsed 0) (mark-time state)
      (< elapsed 3000) (let [{:keys [molecule-particles elapsed] :as new-state} (emit-and-move-molecules panel-height state)
                             _ (om/update-state! component assoc :local-particles molecule-particles)]
                         (when (moles/ten-second-mark? elapsed)
                           (colour-change-fn elapsed))
                         new-state)
      (= elapsed 3000) (let [new-state (start-over state)
                             _ (om/update-state! component assoc :local-particles [])]
                         new-state))))

;;
;; Plain React component, that re-renders based on value of :molecule-particles, that is kept in local state
;;
(defui ^:once Molecules
  Object
  (componentDidMount [this]
    (let [{:keys [colour-change-fn panel-height]} (om/props this)
          _ (assert panel-height)
          _ (assert colour-change-fn)
          _ (assert (fn? colour-change-fn))]
      (go-loop [state {:elapsed 0 :molecule-particles []}]
               (let [_ (<! (timeout moles/wait-time))
                     new-state (to-next-state this panel-height colour-change-fn state)]
                 (recur new-state)))))
  (render [this]
    (let [particles (om/get-state this :local-particles)
          {:keys [panel-height]} (om/props this)
          ;_ (println "In render with " (count particles))
          ]
      (dom/svg #js{:className "back"
                   :opacity   "0.85"
                   :height    (str panel-height "px")
                   :width     (str moles/panel-width "px")}
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
  static om/Ident
  (ident [_ {:keys [id]}] [:signature/by-id id])
  static om/IQuery
  (query [_] [:id :name :company :phone :email])
  Object
  (render [this]
    (let [{:keys [name company phone email]} (om/props this)
          style-js #js{:float "right" :width "210px"}]
      (dom/div #js{:className "tubes-general-container"}
               (dom/div #js{:style style-js} name)
               (dom/br nil)
               (when company
                 (dom/div #js{:className "fa fa-building-o fa-1x" :style style-js} (str " " company))
                 (dom/br nil))
               (dom/div #js{:className "fa fa-phone fa-1x" :style style-js} (str " " phone))
               (dom/br nil)
               (dom/div #js{:className "fa fa-envelope fa-1x" :style style-js} (str " " email))
               (dom/br nil) (dom/br nil)))))
(def ui-signature (om/factory Signature))

(defui ^:once BackgroundColour
  static om/Ident
  (ident [_ {:keys [id]}] [:bg-colour/by-id id])
  static om/IQuery
  (query [_] [:id :red :green :blue]))

(defui ^:once ShowdownDocument
  static om/IQuery
  (query [_] [:id
              :markdown
              :markup
              :contacts
              :panel-height
              :animation?
              {:signature (om/get-query Signature)}
              {:bg-colour (om/get-query BackgroundColour)}])
  static om/Ident
  (ident [_ {:keys [id]}] [:doc/by-id id])
  Object
  (colour-change [this elapsed]
    (om/transact! this `[(app/bg-colour-change {:seconds-elapsed ~(/ elapsed moles/fps)}) :doc/by-id]))
  (render [this]
    (let [{:keys [markup signature bg-colour panel-height animation?]} (om/props this)
          ;_ (println "panel height coming thru: " panel-height)
          ;_ (println "bg-colour coming thru: " bg-colour)
          {:keys [red green blue]} bg-colour
          red (or red (moles/red-pulse 1))
          green (or green (moles/green-pulse 1))
          blue (or blue (moles/blue-pulse 1))
          background-fill (str "rgba(" red "," green "," blue ",0.3)")
          titled-markup (centre-first-heading markup)
          ]
      (dom/div #js{:className "container" :style #js{:width  (str moles/panel-width "px")
                                                     :height (str panel-height "px")}}
               (when animation?
                 (ui-molecules {:colour-change-fn #(.colour-change this %)
                                :panel-height     panel-height}))
               (dom/div #js{:className "front" :style #js{:backgroundColor background-fill}}
                        (dom/div #js{:className "inner-front"}
                                 (dom/div #js {:dangerouslySetInnerHTML #js {:__html titled-markup}} nil)
                                 (ui-signature signature)))))))
(def ui-showdown-document (om/factory ShowdownDocument {:keyfn :id}))

(defn boolean? [v]
  (or (true? v) (false? v)))

;;
;; Needs to update app/login-info in app state
;; This is completely wrong but will do for now - later will leave contacts on the server and so do a mutation that
;; goes to the server to change authenicated?, which resides on the server, and we have a read that brings
;; authenticated? into here. Wait till the Untangled reference documentation is finished.
;;
;; For now just work out the result and transact! so that authenicated? in client gets changed
;;
(defn login-process! [component only-ph-for-pw? un pw contacts]
  (let [_ (assert (boolean? only-ph-for-pw?))
        user-id (str/trim un)
        pass-id (str/trim pw)
        _ (assert (pos? (count contacts)))
        ]
    (println "Login transact! for " user-id pass-id " from " (count contacts))
    (if (or (str/blank? user-id) (str/blank? pass-id))
      (js/alert "Please enter user-id and pass-id first")
      (let [contact (some (fn [c] (when (u/=ignore-case (:first c) user-id) c)) contacts)
            pw-match? (and contact (or (and (not only-ph-for-pw?) (u/=ignore-case (:last contact) pass-id))
                                       (some #(u/phone-match? pass-id %) (:phones contact))))
            okay? pw-match?]
        (when okay?
          (om/transact! component '[(app/authenticate)]))))))
