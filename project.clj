(defproject district0x/district-server-smart-contracts "1.0.17-SNAPSHOT"
  :description "district0x server module for handling smart-contracts"
  :url "https://github.com/district0x/district-server-smart-contracts"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [
                 [cljs-web3 "0.19.0-0-8"]
                 [district0x/async-helpers "0.1.1"]
                 [district0x/district-server-config "1.0.1"]
                 [district0x/district-server-web3 "1.0.1"]
                 [mount "0.1.11"]
                 [org.clojure/clojurescript "1.10.520"]
                 [org.clojure/core.match "0.3.0-alpha4"]
                 ]

  :plugins [[lein-npm "0.6.2"]
            [lein-doo "0.1.8"]]

  :npm {:devDependencies [[jsedn "0.4.1"]
                          [ws "2.0.1"]]}

  ;; :source-paths ["src"]

  :clean-targets ^{:protect false} ["target" "tests-compiled"]

  :profiles {:dev {:dependencies [[lein-doo "0.1.8"]
                                  [org.clojure/clojure "1.10.1"]]
                   :source-paths ["test" "src"]
                   :resource-paths ["resources"]}}

  :deploy-repositories [["snapshots" {:url "https://clojars.org/repo"
                                      :username :env/clojars_username
                                      :password :env/clojars_password
                                      :sign-releases false}]
                        ["releases"  {:url "https://clojars.org/repo"
                                      :username :env/clojars_username
                                      :password :env/clojars_password
                                      :sign-releases false}]]

  :release-tasks [["vcs" "assert-committed"]
                  ["change" "version" "leiningen.release/bump-version" "release"]
                  ["deploy"]]

  :cljsbuild {:builds [{:id "nodejs-tests"
                        :source-paths ["src" "test"]
                        :compiler {:main "tests.runner"
                                   :output-to "tests-compiled/run-tests.js"
                                   :output-dir "tests-compiled"
                                   :target :nodejs
                                   :optimizations :none
                                   :source-map true}}]})
