(defproject wandering "1.0.0-SNAPSHOT"
  :description "Demo"
  :url ""
  :license {:name "MIT"
            :url  "https://opensource.org/licenses/MIT"}

  :dependencies [[org.clojure/clojure "1.9.0-alpha14"]
                 [org.clojure/clojurescript "1.9.456"]
                 [org.omcljs/om "1.0.0-alpha47"]
                 [navis/untangled-client "0.6.0" :exclusions [cljsjs/react org.omcljs/om]]
                 [navis/untangled-server "0.6.2"]
                 [navis/untangled-spec "0.3.9"]
                 [com.taoensso/timbre "4.3.1"]
                 [commons-codec "1.10"]
                 [binaryage/devtools "0.5.2" :scope "test"]
                 [figwheel-sidecar "0.5.3-1" :exclusions [ring/ring-core joda-time org.clojure/tools.reader com.stuartsierra/component]]
                 [endophile "0.1.2"]
                 [reagent "0.6.0-rc"]
                 [markdown-clj "0.9.89"]
                 [cljsjs/showdown "1.4.2-0"]
                 [org.clojure/core.async "0.2.371"]
                 [com.andrewmcveigh/cljs-time "0.3.14"]
                 [hiccup "1.0.5"]
                 [org.clojure/tools.namespace "0.2.11"]
                 ]

  :plugins [[lein-cljsbuild "1.1.4"]
            [lein-less "1.7.5"]]

  :source-paths ["src/server"]

  :jvm-opts ["-server" "-Xmx1024m" "-Xms512m" "-XX:-OmitStackTraceInFastThrow"]
  :clean-targets ^{:protect false} ["target"]

  :cljsbuild {:builds
              [{:id           "marketing"
                :source-paths ["src/client"]
                ;; I think if optimizations becomes advanced then we will no longer need a main
                :compiler     {:main                 app.main
                               ;; :asset-path           "wandering/js/prod"
                               :output-to            "resources/public/marketing/js/main.js"
                               :output-dir           "resources/public/marketing/js/prod_out"
                               :optimizations        :whitespace
                               :parallel-build       false
                               :verbose              false
                               :recompile-dependents true
                               :source-map-timestamp true}}
               {:id           "uneasy"
                :source-paths ["src/client"]
                ;; I think if optimizations becomes advanced then we will no longer need a main
                :compiler     {:main                 app.main
                               ;; :asset-path           "wandering/js/prod"
                               :output-to            "resources/public/uneasy/js/main.js"
                               :output-dir           "resources/public/uneasy/js/prod_out"
                               :optimizations        :whitespace
                               :parallel-build       false
                               :verbose              false
                               :recompile-dependents true
                               :source-map-timestamp true}}]}

  :less {:source-paths ["less/app.main.less"]
         :target-path  "resources/public/css/app.css"}

  :main app.main
  :aot [app.main]

  )

