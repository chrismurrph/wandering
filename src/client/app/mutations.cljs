(ns app.mutations
  (:require [cljsjs.showdown]
            [untangled.client.mutations :as m]
            [app.molecules :as moles]))

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
                                (assoc-in [:plan/by-id 1 :red] (moles/red-pulse seconds-elapsed))
                                (assoc-in [:plan/by-id 1 :green] (moles/green-pulse seconds-elapsed))
                                (assoc-in [:plan/by-id 1 :blue] (moles/blue-pulse seconds-elapsed))))))})

(defn convert-to-html [markdown]
  ;; note the syntax below: js/VarFromExternsFile.property
  ;; the dot on the end is the usual Clojure interop syntax: (Constructor. constructor-arg constructor-arg)
  ;; #js {:tables true}
  (let [converter (js/showdown.Converter.)                  ;;#js {:tables true}
        _ (.setOption converter "tables" true)
        ]
    ;; methods you call will generally need to be called out as prototype values in the externs
    (.makeHtml converter markdown)))

(defmethod m/mutate 'fetch/plan-loaded
  [{:keys [state]} _ _]
  {:action (fn []
             (let [idents (get @state :imported-plans)
                   markdown (get-in @state [:plan/by-id 1 :markdown])
                   ;_ (println "markdown: " markdown)
                   text (convert-to-html markdown)
                   ;_ (println (str "HTML: " text))
                   ]
               (swap! state (fn [st]
                              (-> st
                                  (assoc :plans idents)
                                  (dissoc :imported-plans)
                                  (assoc-in [:plan/by-id 1 :html-text] text))))))})
