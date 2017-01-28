(ns app.bg-colour)

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

