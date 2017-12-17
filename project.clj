(defproject district0x/district-server-smart-contracts "1.0.0"
  :description "district0x server component for handling smart-contracts"
  :url "https://github.com/district0x/district-server-smart-contracts"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[cljs-web3 "0.19.0-0-8"]
                 [district0x/district-server-config "1.0.0"]
                 [district0x/district-server-web3 "1.0.0"]
                 [mount "0.1.11"]
                 [org.clojure/clojurescript "1.9.946"]]

  :npm {:dependencies [[deasync "0.1.11"]]})
