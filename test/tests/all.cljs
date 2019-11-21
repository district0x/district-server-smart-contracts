(ns tests.all
  (:require [cljs-web3-next.eth :as web3-eth]
            [cljs.core.async :refer [go <!] :as async]
            [cljs-web3-next.core :as web3-core]
            [cljs.test :refer-macros [deftest use-fixtures is testing async]]
            [clojure.string :as string]
            [district.server.smart-contracts :as smart-contracts]
            [district.server.web3 :refer [web3]]
            [mount.core :as mount]
            [taoensso.timbre :as log]
            [tests.smart-contracts-test]
            [district.shared.async-helpers :as async-helpers]))

(async-helpers/extend-promises-as-channels!)

(use-fixtures
  :each
  {:before (fn []
             (-> (mount/with-args
                   {:web3 {:url "ws://127.0.0.1:8545"}
                    :smart-contracts {:contracts-var #'tests.smart-contracts-test/smart-contracts}})
                 (mount/start)))
   :after (fn []
            (mount/stop))})

(deftest test-smart-contracts
  (async done
         (go
           (let [events {:my-contract/on-counter-incremented [:my-contract :onCounterIncremented]
                         :my-contract/on-special-event [:my-contract :onSpecialEvent]}
                 my-contract-address (smart-contracts/contract-address :my-contract)
                 connected? (<! (web3-eth/is-listening? @web3))
                 block-number (<! (web3-eth/get-block-number @web3))
                 event-emitter (smart-contracts/subscribe-events :my-contract
                                                                 :onCounterIncremented
                                                                 {:from-block block-number
                                                                  :to-block "latest"}
                                                                 [(fn [error {:keys [:args :event] :as tx}]
                                                                    (is (= "2" (:the-counter args)))
                                                                    (log/debug "new event subscribe-events/callback" event))])
                 one (<! (smart-contracts/contract-call :my-contract :counter))
                 five (<! (smart-contracts/contract-call :my-contract :my-plus [2 3]))
                 increment-counter-tx (<! (smart-contracts/contract-send :my-contract :increment-counter [1] {:gas 5000000}))
                 special-event-tx (<! (smart-contracts/contract-send :my-contract :fire-special-event [2] {:gas 500000}))
                 past-events (<! (smart-contracts/replay-past-events-in-order events (fn [error {:keys [:args :event]}]
                                                                                       (case event
                                                                                         :on-counter-incremented
                                                                                         (is (= "2" (:the-counter args)))

                                                                                         :on-special-event
                                                                                         (is (= "2" (:some-param args))))

                                                                                       (log/debug "replaying past event" event))
                                                                              {:from-block (inc block-number)
                                                                               :to-block "latest"
                                                                               :on-finish (fn []
                                                                                            (log/debug "Finished replaying past events"))}))
                 ;; forwarder tests
                 target (<! (smart-contracts/contract-call :my-contract :target))
                 set-target-tx (<! (smart-contracts/contract-send :my-contract-fwd :set-target [my-contract-address] {:ignore-forward? true
                                                                                                                      :gas 500000}))
                 forwarder-target (<! (smart-contracts/contract-call :my-contract-fwd :target))
                 set-counter-tx (<! (smart-contracts/contract-send [:my-contract :my-contract-fwd] :set-counter [24] {:gas 500000}))
                 counter24 (<! (smart-contracts/contract-call [:my-contract :my-contract-fwd] :counter))

                 ;; Direct way to use forwarders, assuming :forwards-to is defined in smart_contracts.cljs
                 forwarder-event-emitter (smart-contracts/subscribe-events :my-contract-fwd
                                                                           :onCounterIncremented
                                                                           {:from-block (<! (web3-eth/get-block-number @web3))
                                                                            :to-block "latest"}
                                                                           [(fn [error {:keys [:args :event] :as tx}]
                                                                              (log/debug "forwarder event" event))])


                 set-counter-tx (<! (smart-contracts/contract-send :my-contract-fwd :set-counter [28] {:gas 500000}))
                 increment-counter-tx (<! (smart-contracts/contract-send :my-contract-fwd :increment-counter [1] {:gas 5000000}))
                 counter29 (<! (smart-contracts/contract-call :my-contract-fwd :counter))
                 cleanup-tx (<! (smart-contracts/contract-send :my-contract :set-counter [1] {:gas 5000000}))]

             (is (true? connected?))

             (is (false? (empty? (smart-contracts/contract-abi :my-contract))))
             (is (false? (empty? (smart-contracts/contract-bin :my-contract))))
             (is (= (smart-contracts/contract-name :my-contract) "MyContract"))
             (is (not= my-contract-address "0x0000000000000000000000000000000000000000"))
             (is (= (:name (smart-contracts/contract-by-address (smart-contracts/contract-address :my-contract))) "MyContract"))

             (is (= "1" one))
             (is (= "5" five))

             (is (= "0xbeefbeefbeefbeefbeefbeefbeefbeefbeefbeef" (string/lower-case target)))
             (is (= my-contract-address (string/lower-case forwarder-target)))
             (is (= "24" counter24))
             (is (= "29" counter29))

             (done)))))
