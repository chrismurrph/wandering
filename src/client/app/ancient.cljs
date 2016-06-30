(ns app.ancient
  (:require [om.dom :as dom]
            [app.molecules :as moles]))

(def diameter 30)

(defn conv->svg [molecule-symbol]
  (let [_ (println molecule-symbol)]
    molecule-symbol))

(defn render-molecule-symbol [molecule-symbol]
  (let [[r g b] (:mole-fill molecule-symbol)
        saturation (moles/calc-distance-saturation molecule-symbol)]
    ;(q/fill r g b saturation)
    ;(q/text (:symbol-txt molecule-symbol) 0 0)
    (dom/rect (clj->js (conv->svg molecule-symbol)))
    ))

;;
;; Use a minus number for margin if you want symbols to disappear while still on the screen
;;
(defn on-screen? [x y width height]
  (let [margin diameter
        res (and (<= (- margin) x (+ margin width))
                 (<= (- margin) y (+ margin height)))]
    ;(when-not res (u/log "Going off the screen"))
    res))

(defn draw-entity [entity]
  (let [{:keys [x y angle]} entity
        ;z (:z entity)
        screen-x x
        screen-y y]
    (when (on-screen? screen-x screen-y 20 20)
      ;(q/push-matrix)
      ;(q/translate screen-x screen-y)
      ;(q/rotate angle)
      (render-molecule-symbol entity)
      ;(q/pop-matrix)
      )))

(defn draw-state [state]
  ;(u/log "In draw-state")
  ;(q/background (current-bg-colour))
  ;(q/no-stroke)
  ;(u/log "molecule-particles count " (count (:molecule-particles state)))
  (doseq [molecule (:molecule-particles state)]
    (draw-entity molecule)))
