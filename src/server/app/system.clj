(ns app.system
  (:require
    [clojure.data.json :as json]
    [untangled.server.core :as core]
    [app.api :as api]
    [om.next.server :as om]
    [taoensso.timbre :as timbre]
    [om.next.impl.parser :as op]
    [com.stuartsierra.component :as component]
    [hiccup.core :as hiccup]))

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

;; resources/public is assumed (see /css and /js):
(defn handle-index-1 [env match]
  (hiccup/html
    [:head
     [:meta {:charset "UTF-8"}]
     [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
     [:link {:rel "stylesheet" :href "/reconnect/css/base.css" :type "text/css"}]
     [:link {:rel "stylesheet" :href "/reconnect/css/pure.css" :type "text/css"}]
     [:link {:rel "stylesheet" :href "/reconnect/css/grids-responsive.css" :type "text/css"}]
     [:link {:rel "stylesheet" :href "/reconnect/css/app.css" :type "text/css"}]
     [:link {:rel "stylesheet" :href "/reconnect/css/font-awesome.min.css" :type "text/css"}]
     [:link {:rel "stylesheet" :href "http://cdn.leafletjs.com/leaflet/v0.7.7/leaflet.css" :type "text/css"}]
     [:script {:src "http://cdn.leafletjs.com/leaflet/v0.7.7/leaflet.js" :charset "utf-8"}]
     [:script {:src "//d3js.org/d3.v3.min.js" :charset "utf-8"}]]
    [:body
     [:div {:id "mapid"}]
     [:div {:id "main-app-area"}]
     [:script {:src "/reconnect/js/main.js" :type "text/javascript"}]]))

;; <div id="app"></div>
;; <script src="js/dev-app.js"></script>
(defn web-entry [{:keys [deploy-type]}]
  (let [prod? (= deploy-type :prod)]
    (fn [env match]
      (let [port (-> env :filesystem :config :value :port)
            portify (fn [s] (if prod? (str ":" port "/" s) s))
            js (portify "wandering/js/main.js")]
        {:status  200
         :headers {"Content-Type" "text/html"}
         :body    (hiccup/html [:head
                                [:meta {:charset "UTF-8"}]
                                [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
                                [:title "Document Site"]
                                [:link {:rel "icon" :href (portify "wandering/favicon.ico") :type "image/x-icon"}]
                                [:link {:rel "shortcut icon" :href (portify "wandering/favicon.ico") :type "image/x-icon"}]
                                [:link {:rel "stylesheet" :href (portify "wandering/css/app.css")}]
                                [:link {:rel "stylesheet" :href (portify "wandering/css/font-awesome.min.css")}]
                                ]
                               [:body
                                [:div {:id "app"}]
                                [:script {:src js}]])}))))

#_(defn handle-index [env match]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (hiccup/html [:head
                       [:meta {:charset "UTF-8"}]
                       [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]]
                      [:body [:div "some content"]])})

(defn make-system [m args]
  (let [arg (first args)
        _ (println "arg from cmd line is: <" arg ">")
        edn-file-name (if arg (str "/usr/local/etc/" arg ".edn") "/usr/local/etc/wandering.edn")]
    (core/make-untangled-server
      :config-path edn-file-name
      :parser (om/parser {:read logging-query :mutate logging-mutate})
      :parser-injections #{:filesystem}
      :components {:filesystem (build-filesystem-reader)}
      :extra-routes {:routes   ["" {["/wandering"] :index}]
                     :handlers {:index (web-entry m)}})))
