(ns app.mutations
  (:require [cljsjs.showdown]
            [untangled.client.mutations :as m]
            [app.molecules :as moles]))

(defmethod m/mutate 'app/elapsed
  [{:keys [state]} _ {:keys [elapsed]}]
  {:action (fn []
             (swap! state assoc :app/elapsed elapsed))})

(defmethod m/mutate 'app/bg-colour-change
  [{:keys [state]} _ {:keys [seconds-elapsed]}]
  {:action (fn []
             (swap! state (fn [st]
                            (let [bg-colour {:red   (moles/red-pulse seconds-elapsed)
                                             :green (moles/green-pulse seconds-elapsed)
                                             :blue  (moles/blue-pulse seconds-elapsed)}]
                              (assoc-in st [:doc/by-id 1 :bg-colour] bg-colour)))))})

(defn- convert-to-html [md]
  ;; note the syntax below: js/VarFromExternsFile.property
  ;; the dot on the end is the usual Clojure interop syntax: (Constructor. constructor-arg constructor-arg)
  ;; #js {:tables true}
  (let [converter (js/showdown.Converter.)                  ;;#js {:tables true}
        _ (.setOption converter "tables" true)
        ]
    ;; methods you call will generally need to be called out as prototype values in the externs
    (.makeHtml converter md)))

(defmethod m/mutate 'app/authenticate
  [{:keys [state]} _ {:keys [special-person?]}]
  (let [kw (if special-person? :alternative-markdown :regular-markdown)
        markdown (get-in @state [:doc/by-id 1 kw])
        text (convert-to-html markdown)
        _ (println (str "special person " special-person? " so used: " kw))
        ]
    {:action (fn []
               (swap! state #(-> %
                                 (assoc-in [:login-dlg/by-id 2 :app/authenticated?] true)
                                 (assoc-in [:doc/by-id 1 :special-person?] special-person?)
                                 (assoc-in [:doc/by-id 1 :markup] text))))}))

(defmethod m/mutate 'fetch/init-state-loaded
  [{:keys [state]} _ _]
  {:action (fn []
             (let [docs-idents (get @state :imported-docs)
                   logins-idents (get @state :imported-logins)
                   ;special-person? (get-in @state [:doc/by-id 1 :special-person?])
                   ;kw (if special-person? :alternative-markdown :regular-markdown)

                   ;_ (println (str "HTML: " text))
                   ]
               (swap! state #(-> %
                                 (assoc :app/docs docs-idents)
                                 (dissoc :imported-docs)
                                 (assoc :app/login-info logins-idents)
                                 (dissoc :imported-logins)))))})
