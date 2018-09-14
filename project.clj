(defproject district0x/district-server-smart-contracts "1.0.9-SNAPSHOT"
  :description "district0x server module for handling smart-contracts"
  :url "https://github.com/district0x/district-server-smart-contracts"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[cljs-web3 "0.19.0-0-8"]
                 [district0x/district-server-config "1.0.1"]
                 [district0x/district-server-web3 "1.0.1"]
                 [mount "0.1.11"]
                 [org.clojure/core.match "0.3.0-alpha4"]
                 [org.clojure/clojurescript "1.9.946"]]

  :plugins [[lein-npm "0.6.2"]
            [lein-doo "0.1.8"]
            [lein-solc "1.0.1-1"]]
  
  :npm {:dependencies [[deasync "0.1.11"]]
        :devDependencies [[ws "2.0.1"]]}
  
  ;; :figwheel {:server-port 4674}

  :solc {:src-path "resources/public/contracts/src"
         :build-path "resources/public/contracts/build"
         :solc-err-only true
         :verbose false
         :wc true
         :contracts :all}

  :clean-targets ^{:protect false} ["target" "tests-compiled"]
  
  :profiles {:dev {:dependencies [[org.clojure/clojure "1.8.0"]
                                  #_[com.cemerick/piggieback "0.2.2"]
                                  #_[figwheel-sidecar "0.5.14"]
                                  #_[org.clojure/tools.nrepl "0.2.13"]]
                   ;;:source-paths ["dev"]
                   }}

  :cljsbuild {:builds [{:id "tests"
                        :source-paths ["src" "test"]
                        ;; :figwheel {:on-jsload "tests.runner/-main"}
                        :compiler {:main "tests.runner"
                                   :output-to "tests-compiled/run-tests.js"
                                   :output-dir "tests-compiled"
                                   :target :nodejs
                                   :optimizations :none
                                   :source-map true}}]})
