(ns app.system
  (:require
    [untangled.server.core :as core]
    [app.api :as api]
    [om.next.server :as om]
    [taoensso.timbre :as timbre]
    [om.next.impl.parser :as op]
    [com.stuartsierra.component :as component]))

(defn read-raw-plan [file-name]
  (let [md-str-in (slurp file-name)]
    md-str-in))

(defrecord MarkdownReader [config]
  component/Lifecycle
  (start [this]
    (let [{:keys [path-to-mkd-file] :as value} (:value config)
          _ (println "SEE: " value)
          _ (assert path-to-mkd-file (str "Got nufin from config: " config))]
      (assoc this :markdown-text
                  (read-raw-plan path-to-mkd-file))))
  (stop [this] this))

(defn build-markdown-reader []
  (component/using
    (map->MarkdownReader {})
    [:config]))

(defn logging-mutate [env k params]
  (timbre/info "Mutation Request: " k)
  (api/apimutate env k params))

(defn logging-query [{:keys [ast] :as env} k params]
  (timbre/info "Query: " (op/ast->expr ast))
  (api/api-read env k params))

(defn make-system [app-config-path]
  (core/make-untangled-server
    :config-path app-config-path
    :parser (om/parser {:read logging-query :mutate logging-mutate})
    :parser-injections #{:markdown}
    :components {:markdown (build-markdown-reader)}))
