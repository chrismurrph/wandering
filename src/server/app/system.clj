(ns app.system
  (:require
    [clojure.data.json :as json]
    [untangled.server.core :as core]
    [app.api :as api]
    [om.next.server :as om]
    [taoensso.timbre :as timbre]
    [om.next.impl.parser :as op]
    [com.stuartsierra.component :as component]))

(defn boolean? [v]
  (or (true? v) (false? v)))

(defn read-raw-plan [file-name]
  (let [md-str-in (slurp file-name)]
    md-str-in))

(defn read-contacts [file-name]
  (let [contacts (slurp file-name)
        res (json/read-str contacts :key-fn clojure.core/keyword)]
    res))

(defrecord FileSystemReader [config]
  component/Lifecycle
  (start [this]
    (let [{:keys [path-to-mkd-file path-to-contacts-file name company title phone email panel-height animation?] :as value} (:value config)
          _ (assert company)
          _ (assert title)
          _ (assert panel-height)
          _ (assert (boolean? animation?))
          _ (assert path-to-mkd-file (str "Got nufin from config for path-to-mkd-file: " value))
          _ (assert path-to-contacts-file (str "Got nufin from config for path-to-contacts-file: " value))
          markdown (read-raw-plan path-to-mkd-file)
          contacts (read-contacts path-to-contacts-file)
          _ (assert (pos? (count contacts)))
          ]
      (assoc this :title title
                  :markdown markdown
                  :contacts contacts
                  :panel-height panel-height
                  :animation? animation?
                  :signature {;:id (gensym)
                              :name name
                              :company company
                              :phone phone
                              :email email})))
  (stop [this] this))

(defn build-filesystem-reader []
  (component/using
    (map->FileSystemReader {})
    [:config]))

(defn logging-mutate [env k params]
  (timbre/info "Mutation Request: " k)
  (api/apimutate env k params))

(defn logging-query [{:keys [ast] :as env} k params]
  (timbre/info "Query: " (op/ast->expr ast))
  (api/api-read env k params))

(defn make-system [args]
  (let [arg (first args)
        _ (println "arg from cmd line is: <" arg ">")
        edn-file-name (if arg (str "/usr/local/etc/" arg ".edn") "/usr/local/etc/wandering.edn")]
    (core/make-untangled-server
      :config-path edn-file-name
      :parser (om/parser {:read logging-query :mutate logging-mutate})
      :parser-injections #{:filesystem}
      :components {:filesystem (build-filesystem-reader)})))
