(ns app.main
  (:require
    [com.stuartsierra.component :as component]
    [app.system :as sys])
  (:gen-class))

; Production entry point.

(defn -main
  "Main entry point for the server"
  [& args]
  (let [system (sys/make-system args)]
    (component/start system)))
