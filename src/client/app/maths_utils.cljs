(ns app.maths-utils)

(defn log [& txts]
  (.log js/console (apply str txts)))

(defn vector->radians
  [[x y]]
  (. js/Math (atan2 y x)))

(defn translate-v2
  ([[x y] [dx dy]] (translate-v2 [x y] [dx dy] +))
  ([[x y] [dx dy] op] [(op x dx) (op y dy)]))

(defn abs
  [[x y]] [(max x (- x)) (max y (- y))])

(defn calc-direction [from-zero-vec]
  (let [delta (translate-v2 [0 0] from-zero-vec +)
        dir (vector->radians delta)]
    ;(log "Centre is " centre ", and vec is " vec ", so delta is " delta))
    dir))

(defn radians->vector
  [angle constant]
  [(* constant (. js/Math (cos angle)))
   (* constant (. js/Math (sin angle)))])

(comment (defn radians->degrees [radians]
           (* radians 57.2958)))

;; degrees = radians * (180/pi)
(defn radians->degrees [radians]
  (* radians (/ 180 js/Math.PI)))

(defn chance-one-in
  "If you gave 100 as the argument then there would be a 1% chance of getting true, and 99% chance of getting false"
  [n]
  (if ( = n 0)
    false
    (= 0 (rand-int n))))

(defn coin-flip
  "Randomly returns either true or false"
  []
  (chance-one-in 2))

;;
;; Note that there will be a tendency to not quite go to the sample-size due to the de-duping effect of creating
;; a set. In those cases we recurse until the difference is made up.
;;
(defn random-ints
  [sample-size pop-size]
  (let [res (set (take sample-size (repeatedly #(rand-int pop-size))))
        diff (- sample-size (count res))]
    (if (> diff 0)
      (set (concat res (random-ints diff pop-size)))
      res)))

(defn random-floats
  ([sample-size lowest highest]
   (let [randoms (random-floats sample-size)
         diff (- highest lowest)]
     (map #(+ (* diff %) lowest) randoms)))
  ([sample-size]
   (let [res (set (take sample-size (repeatedly #(rand))))
         missing (- sample-size (count res))]
     (if (> missing 0)
       (set (concat res (random-floats missing)))
       res))))

;(defn random-float
;  [lowest highest]
;  first (random-floats 1 lowest highest))

(defn random-float [low high]
  (let [diff (- high low)]
    (+ low (rand diff))))

(defn random-angle
  []
  (random-float 0 (* js/Math.PI 2)))

(defn rotate-angle
  [angle amt]
  (let [circumference (* js/Math.PI 2)
        maybe-new-angle (rem (+ amt angle) circumference)]
    (if(neg? maybe-new-angle)
         (+ circumference maybe-new-angle)
         maybe-new-angle)))

(defn least-of [a b] (if (< a b) a b))

;; Assumes that it is a square, so if not have to do x and y separately
;; Intention is that the result is compared to half the width (width == height)
;; brightness = half-constant - closest-edge-distance
;; ... thus when in centre brightness(saturation) will be very small
(defn closest-edge-distance
  [x y width height]
  (let [least-x (least-of x (- width x))
        least-y (least-of y (- height y))]
    (least-of least-x least-y)))


