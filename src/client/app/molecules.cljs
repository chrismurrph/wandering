(ns app.molecules
  (:require [app.maths-utils :as mu]
            [om.dom :as dom]))

(def diameter 30)

;;
;; Use a minus number for margin if you want symbols to disappear while still on the screen
;;
(defn on-screen? [x y width height]
  (let [margin diameter
        res (and (<= (- margin) x (+ margin width))
                 (<= (- margin) y (+ margin height)))]
    ;(when-not res (u/log "Going off the screen"))
    res))

(def width 755)
(def height 3000)
(def centre-distance (/ height 2))

(defn calc-distance-saturation
  [entity]
  (let [{:keys [x y max-saturation]} entity
        edge-distance (mu/closest-edge-distance x y width height)
        brightness (/ (- centre-distance edge-distance) centre-distance)]
    (* brightness max-saturation)))

(defn conv->svg [molecule-symbol]
  (let [_ (println molecule-symbol)]
    molecule-symbol))

(defn render-molecule-symbol [molecule-symbol]
  (let [[r g b] (:col molecule-symbol)
        saturation (calc-distance-saturation molecule-symbol)]
    ;(q/fill r g b saturation)
    ;(q/text (:symbol-txt molecule-symbol) 0 0)
    (dom/rect (clj->js (conv->svg molecule-symbol)))
    ))

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

(defn pulse [low high rate]
  (let [diff (- high low)
        half (/ diff 2)
        mid (+ low half)
        ;s (/ (q/millis) 1000.0)
        ;x (q/sin (* s (/ 1.0 rate)))
        x -1 ; silly number
        ]
    (+ mid (* x half))))

; I don't really understand what this. However it is good because it subtly changes
; the background colour in a way the user won't consciously notice
(defn current-bg-colour []
  (pulse 200 220 15.0)
  (pulse 220 240 40.0)
  (pulse 240 255  5.0))

(defn draw-state [state]
  ;(u/log "In draw-state")
  ;(q/background (current-bg-colour))
  ;(q/no-stroke)
  ;(u/log "molecule-particles count " (count (:molecule-particles state)))
  (doseq [molecule (:molecule-particles state)]
    (draw-entity molecule)))

(def fps 11)
(def wait-time (/ 1000 fps))
(defn one-second-mark? [elapsed]
  (zero? (rem elapsed fps)))
(def gray-saturation 30)                                         ; 200 is normal
(def gray-colour [245,245,220])
(def centre-pos [(/ width 2) (/ height 2)])
(def dark-blue [0,0,139])
(def yellow [255,255,0])
(def black [0,0,0])
(def red [255,0,0])

(defn move-molecule-symbol [molecule-symbol]
  (let [{:keys [x y dir]} molecule-symbol
        [delta-x delta-y] (mu/radians->vector dir 1)
        [new-x new-y] (mu/translate-v2 [x y] [delta-x delta-y])]
    ;(u/log new-x) (u/log new-y)
    (assoc molecule-symbol :x new-x :y new-y)))

;; CO2 dark blue, CO yellow, O2 black, CH4 red
(def gas-infos [
                {:colour dark-blue :text "CO\u2082" :max-saturation 160}
                {:colour yellow :text "CO" :max-saturation 100}
                {:colour black :text "O\u2082" :max-saturation 160}
                {:colour red :text "CH\u2084" :max-saturation 130}])

(defn random-pick
  []
  (let [idx (rand-int 4)]
    ;(u/log "Got " colour-idx)
    (get gas-infos idx)))

;; Future enhancement is for right at start there to be much bigger hatchery area and many more created
;; This way user won't be distracted by seeing them spread to the outside
(def hatchery-area-size 20)
(defn x-val [vec] (first vec))
(defn y-val [vec] (second vec))

;; If one every fps is too much we can be random
;; If need more then we won't use conj but concat(?), and return a vector here
(defn create-molecule-symbol []
  (let [x-random (mu/random-float (- hatchery-area-size) hatchery-area-size)
        y-random (mu/random-float (- hatchery-area-size) hatchery-area-size)
        dir (mu/calc-direction [x-random y-random])
        ;{:keys [x y]} [(+ (x-val centre-pos) x-random)
        ;     (+ (y-val centre-pos) y-random)]
        x (+ (x-val centre-pos) x-random)
        y (+ (y-val centre-pos) y-random)
        pick (random-pick)
        stand-out (mu/chance-one-in 10)]
    {:id (gensym)
     :x x
     :y y
     :dir dir
     :max-saturation (if stand-out (:max-saturation pick) gray-saturation)
     :z (if stand-out 1.0 0.1)
     :col (if stand-out (:colour pick) gray-colour)
     :symbol-txt (:text pick)
     :speed 0.25
     :angle (mu/random-angle)}))

(defn emit-molecule-particles [state]
  (let [num-particles (count (:molecule-particles state))]
    (if (and (< num-particles 100) (mu/chance-one-in 5))
      (update state :molecule-particles conj (create-molecule-symbol))
      state)))
