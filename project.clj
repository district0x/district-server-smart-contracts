(defproject district0x/district-server-smart-contracts "1.0.9"
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
            [lein-solc "1.0.11"]]

  :npm {:dependencies [[deasync "0.1.11"]]
        :devDependencies [[ws "2.0.1"]]}

  :solc {:src-path "resources/public/contracts/src"
         :build-path "resources/public/contracts/build"
         :solc-err-only true
         :verbose false
         :wc true
         :contracts :all
         :abi? false
         :bin? false
         :truffle-artifacts? true}

  :source-paths ["src"]

  :clean-targets ^{:protect false} ["target" "tests-compiled"]

  :profiles {:dev {:dependencies [[org.clojure/clojure "1.9.0"]]
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
                  ["vcs" "commit" "Version ${:version} %s [ci skip]"]
                  ["vcs" "tag" "v" "--no-sign"] ; disable signing and add "v" prefix
                  ["deploy"]]

  :cljsbuild {:builds [{:id "tests"
                        :source-paths ["src" "test"]
                        :compiler {:main "tests.runner"
                                   :output-to "tests-compiled/run-tests.js"
                                   :output-dir "tests-compiled"
                                   :target :nodejs
                                   :optimizations :none
                                   :source-map true}}]})
