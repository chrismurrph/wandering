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

(defn boolean? [v]
  (or (true? v) (false? v)))

