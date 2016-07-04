(ns app.utils
  (:require [clojure.string :as str]))

(defn probe
  ([predicate msg obj]
   (assert (fn? predicate))
   (when-let [res (predicate obj)]
     (println (str (str/upper-case msg) ":\n" obj)))
   obj)
  ([msg obj]
   (probe (constantly true) msg obj)))

(defn probe-off
  ([msg obj]
   obj)
  ([msg predicate obj]
   obj))

(defn digit? [x]
  (re-matches #"[0-9]" x))

(defn space? [x]
  (re-matches #" " x))

(defn all-digits? [coll]
  (every? digit? coll))

(defn boolean? [v]
  (or (true? v) (false? v)))

(defn =ignore-case [s1 s2]
  (= (str/upper-case s1) (str/upper-case s2)))

(defn remove-whitespace [x]
  (apply str (remove space? x)))

(defn phone-match? [ph1 ph2]
  (let [trim1 (remove-whitespace ph1)
        trim2 (remove-whitespace ph2)]
    (println "cf: " trim1 trim2)
    (if (and (all-digits? trim1) (all-digits? trim2))
      (= trim1 trim2)
      false)))

