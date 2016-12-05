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
    (let [{:keys [path-to-mkd-file alternative-mkd-file path-to-contacts-file name company title phone email panel-height animation? only-ph-for-pw?] :as value} (:value config)
          ;; Don't want to have to have
          ;;_ (assert company)
          _ (assert title)
          _ (assert panel-height)
          _ (assert (boolean? animation?))
          _ (assert (boolean? only-ph-for-pw?))
          _ (assert path-to-mkd-file (str "Got nufin from config for path-to-mkd-file: " value))
          _ (assert path-to-contacts-file (str "Got nufin from config for path-to-contacts-file: " value))
          regular-markdown (read-raw-plan path-to-mkd-file)
          alternative-markdown (read-raw-plan alternative-mkd-file)
          contacts (read-contacts path-to-contacts-file)
          _ (assert (pos? (count contacts)))
          ]
      (assoc this :title title
                  :only-ph-for-pw? only-ph-for-pw?
                  :regular-markdown regular-markdown
                  :alternative-markdown alternative-markdown
                  :path-to-mkd-file path-to-mkd-file
                  :alternative-mkd-file alternative-mkd-file
                  :contacts contacts
                  :panel-height panel-height
                  :animation? animation?
                  :signature {:name name
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

(defn app-name-ify [deploy-type app-name s]
  (let [prod? (= deploy-type :prod)]
    (if prod? (str app-name "/" s) (str "wandering/" s))))

;; resources/public is assumed (see /css and /js):
;; <div id="app"></div>
;; <script src="js/dev-app.js"></script>
(defn web-entry [app-name {:keys [deploy-type]}]
  (fn [env match]
    (let [namify (partial app-name-ify deploy-type app-name)
          js (namify "js/main.js")]
      {:status  200
       :headers {"Content-Type" "text/html"}
       :body    (hiccup/html [:head
                              [:meta {:charset "UTF-8"}]
                              [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
                              [:title "Document Site"]
                              [:link {:rel "icon" :href (namify "favicon.ico") :type "image/x-icon"}]
                              [:link {:rel "shortcut icon" :href (namify "favicon.ico") :type "image/x-icon"}]
                              [:link {:rel "stylesheet" :href (namify "css/app.css")}]
                              [:link {:rel "stylesheet" :href (namify "css/font-awesome.min.css")}]
                              ]
                             [:body
                              [:div {:id "app"}]
                              [:script {:src js}]])})))

#_(defn handle-index [env match]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (hiccup/html [:head
                       [:meta {:charset "UTF-8"}]
                       [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]]
                      [:body [:div "some content"]])})

(defn make-system [m args]
  (let [app-name (first args)
        _ (println (str "arg from cmd line is: <" app-name ">"))
        edn-file-name (if app-name (str "/usr/local/etc/" app-name ".edn") "/usr/local/etc/wandering.edn")]
    (core/make-untangled-server
      :config-path edn-file-name
      :parser (om/parser {:read logging-query :mutate logging-mutate})
      :parser-injections #{:filesystem}
      :components {:filesystem (build-filesystem-reader)}
      :extra-routes {:routes   ["" {[(str "/" app-name)] :index}]
                     :handlers {:index (web-entry app-name m)}}
      :app-name app-name)))
