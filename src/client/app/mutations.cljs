(ns app.mutations
  (:require [untangled.client.mutations :as m]
            [untangled.client.core :as uc]
            [app.ui :as ui]
            [om.next :as om]))

(defn sin [x]
  (.sin js/Math x))

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

(defmethod m/mutate 'app/elapsed
  [{:keys [state]} _ {:keys [elapsed]}]
  {:action (fn []
             (swap! state (fn [st]
                            (-> st
                                (assoc :elapsed elapsed)))))})

(defmethod m/mutate 'app/bg-colour-change
  [{:keys [state]} _ {:keys [seconds-elapsed]}]
  {:action (fn []
             (swap! state (fn [st]
                            ;(println "elapsed: " seconds-elapsed)
                            (-> st
                                (assoc-in [:plan/by-id 1 :red] (red-pulse seconds-elapsed))
                                (assoc-in [:plan/by-id 1 :green] (green-pulse seconds-elapsed))
                                (assoc-in [:plan/by-id 1 :blue] (blue-pulse seconds-elapsed))))))})

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

(defmethod m/mutate 'fetch/plan-loaded
  [{:keys [state]} _ _]
  {:action (fn []
             (let [idents (get @state :imported-plans)
                   markdown (get-in @state [:plan/by-id 1 :markdown])
                   _ (println "markdown: " markdown)
                   ;; Need to convert here:
                   text (convert-to-html markdown)
                   ;_ (println (str "HTML: " text))
                   ]
               (swap! state (fn [st]
                              (-> st
                                  (assoc :plans idents)
                                  (dissoc :imported-plans)
                                  (assoc-in [:plan/by-id 1 :html-text] text))))))})
