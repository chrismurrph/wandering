(ns app.molecules
  (:require [app.maths-utils :as mu]))

(defn sin [x]
  (.sin js/Math x))

(def diameter 50)

;; subtly changes the background colour in a way the user won't consciously notice
(defn- pulse [seconds-elapsed low high rate]
  (let [diff (- high low)
        half (/ diff 2)
        mid (+ low half)
        x (sin (* seconds-elapsed (/ 1.0 rate)))]
    (int (+ mid (* x half)))))

(defn red-pulse [seconds-elapsed] (pulse seconds-elapsed 200 220 15.0))
(defn green-pulse [seconds-elapsed] (pulse seconds-elapsed 220 240 40.0))
(defn blue-pulse [seconds-elapsed] (pulse seconds-elapsed 240 255 5.0))

(def width 955)
(def height 3000)
(def centre-distance (/ height 2))

;;
;; Becomes brighter the further are away. Each type of molecule has a different starting saturation.
;;
(defn calc-distance-saturation
  [entity]
  (let [{:keys [x y max-saturation]} entity
        _ (assert (and x y max-saturation))
        edge-distance (mu/closest-edge-distance x y width height)
        brightness (/ (- centre-distance edge-distance) centre-distance)]
    (* brightness max-saturation)))

(def fps 11)
(def wait-time (/ 1000 fps))
(defn one-second-mark? [elapsed]
  (zero? (rem elapsed fps)))
(defn ten-second-mark? [elapsed]
  (zero? (rem elapsed (* 10 fps))))
(def gray-saturation 30)                                    ; 200 is normal
(def gray-colour [200, 200, 220])
(def centre-pos [(/ width 2) (/ height 2)])
(def dark-blue [0, 0, 139])
(def yellow [213, 225, 49])
(def black [0, 0, 0])
(def red [255, 0, 0])

;;
;; Mutation of local state
;;
(defn move-molecule-symbol [molecule]
  (let [{:keys [x y dir last-degrees-angle change-by-degrees-angle]} molecule
        [delta-x delta-y] (mu/radians->vector dir (:speed molecule))
        [new-x new-y] (mu/translate-v2 [x y] [delta-x delta-y])]
    (-> molecule
        (assoc :x new-x :y new-y :last-degrees-angle (+ last-degrees-angle change-by-degrees-angle)))))

;; CO2 dark blue, CO yellow, O2 black, CH4 red
(def gas-infos [{:colour dark-blue :text "CO\u2082" :max-saturation 160 :speed 1.0}
                {:colour yellow :text "CO" :max-saturation 100 :speed 1.1}
                {:colour black :text "O\u2082" :max-saturation 160 :speed 0.8}
                {:colour red :text "CH\u2084" :max-saturation 130 :speed 0.9}])

(defn random-pick
  []
  (let [idx (rand-int 4)]
    ;(u/log "Got " colour-idx)
    (get gas-infos idx)))

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
    {:id                      (gensym)
     :x                       x
     :y                       y
     :dir                     dir
     :max-saturation          (if stand-out (:max-saturation pick) gray-saturation)
     :z                       (if stand-out 1.0 0.3)
     :mole-fill               (if stand-out (:colour pick) gray-colour)
     :symbol-txt              (:text pick)
     :speed                   (:speed pick)
     :start-degrees-angle     (int (mu/radians->degrees (mu/random-angle)))
     :change-by-degrees-angle (- (mu/random-float 0 1) 0.5)
     }))

(defn emit-molecule-particles [state]
  (let [num-particles (count (:molecule-particles state))]
    (if (and (< num-particles 100) (mu/chance-one-in 5))
      (update state :molecule-particles conj (create-molecule-symbol))
      state)))
