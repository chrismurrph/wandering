(ns app.main
  (:require
    [com.stuartsierra.component :as component]
    [app.system :as sys])
  (:gen-class))

; Production entry point.

(defn -main
  "Main entry point for the server"
  [& args]
  (let [arg (first args)
        _ (println "arg from cmd line is: <" arg ">")
        edn-file-name (if arg (str "/usr/local/etc/" arg ".edn") "/usr/local/etc/wandering.edn")
        system (sys/make-system edn-file-name)]
    (component/start system)))
