(ns tests.all
  (:require
   [cljs-web3.core :as web3]
   [cljs.test :refer-macros [deftest use-fixtures is testing async]]
   [district.server.smart-contracts :as contracts]
   [mount.core :as mount]
   [tests.smart-contracts-test]))

(defn- map-vals [m f]
  (into {} (for [[k v] m] [k (f v)])))

(use-fixtures
  :each
  {:before (fn []
             (-> (mount/with-args
                   {:web3 {:port 8545}
                    :smart-contracts {:contracts-var #'tests.smart-contracts-test/smart-contracts
                                      :print-gas-usage? true
                                      :auto-mining? true}})
                 (mount/start)))
   :after (fn []
            (mount/stop))})


(deftest test-smart-contracts
  (testing "smart contracts test suite"

    (is (false? (empty? (contracts/contract-abi :my-contract))))
    (is (false? (empty? (contracts/contract-bin :my-contract))))

    (is (= (contracts/contract-address :my-contract) "0x0000000000000000000000000000000000000000"))
    (is (= (contracts/contract-name :my-contract) "MyContract"))

    (async done
           (let [ev-filter (partial contracts/create-event-filter :my-contract :on-counter-incremented {})]
             (-> (js/Promise.resolve true)
                 #_(contracts/deploy-smart-contract! :my-contract [1])

                 (.then #(is (not= (contracts/contract-address :my-contract) "0x0000000000000000000000000000000000000000")))

                 (.then #(is (= (:name (contracts/contract-by-address (contracts/contract-address :my-contract)))
                                "MyContract")))

                 (.then #(-> (contracts/contract-call :my-contract :counter)
                             (.then (fn [counter] (is (= 1 (.toNumber counter)))))))

                 (.then #(-> (contracts/contract-call :my-contract :my-plus [2 3])
                             (.then (fn [result] (is (= 5 (.toNumber result)))))))

                 (.then #(-> (contracts/contract-call :my-contract :increment-counter [2] {:gas 500000})
                             (.then (fn [tx-hash]
                                      (contracts/wait-for-tx-receipt tx-hash)))
                             (.then (fn [{:keys [:transaction-hash]}]
                                      (is (= 3 (-> (contracts/contract-event-in-tx transaction-hash :my-contract :on-counter-incremented)
                                                   :args
                                                   :the-counter
                                                   .toNumber)))))))

                 (.then #(-> (contracts/contract-call :my-contract :double-increment-counter [2] {:gas 500000})

                             (.then (fn [tx-hash]
                                      (contracts/wait-for-tx-receipt tx-hash)))

                             (.then (fn [{:keys [:transaction-hash]}]
                                      (is (= [5 7] (map (comp (fn [x] (.toNumber x)) :the-counter :args)
                                                        (contracts/contract-events-in-tx transaction-hash :my-contract :on-counter-incremented))))))))

                 (.then #(-> (contracts/contract-call :my-contract :target)
                             (.then (fn [target]
                                      (is (= "0xbeefbeefbeefbeefbeefbeefbeefbeefbeefbeef" target))))))

                 (.then #(-> (contracts/contract-call :my-contract :counter)
                             (.then (fn [counter]
                                      (is (= 7 (.toNumber counter)))))))

                 (.then (fn []
                          (let [expected-numbers [3 5 7]
                                actual-numbers (atom [])]
                            (contracts/replay-past-events (apply ev-filter [{:from-block 0 :to-block "latest"}])
                                                          (fn [_ {:keys [:args]}]
                                                            (swap! actual-numbers #(into % [(-> args :the-counter .toNumber)])))
                                                          {:on-finish
                                                           (fn []
                                                             (is (= expected-numbers @actual-numbers))
                                                             (done))})))))))))


(deftest test-replay-past-events-in-order
  (testing "test-replay-past-events-in-order"

    (async done
           (-> (js/Promise.resolve true)
               #_(contracts/deploy-smart-contract! :my-contract [1])
               (.then #(contracts/contract-call :my-contract :double-increment-counter [2] {:gas 500000}))
               (.then #(contracts/contract-call :my-contract :fire-special-event [1] {:gas 500000}))
               (.then #(contracts/contract-call :my-contract :fire-special-event [2] {:gas 500000}))
               (.then #(contracts/contract-call :my-contract :increment-counter [3] {:gas 500000}))
               (.then (fn []
                        (let [expected-events [["MyContract" :my-contract :on-counter-incremented {:the-counter 3}]
                                               ["MyContract" :my-contract :on-counter-incremented {:the-counter 5}]
                                               ["MyContract" :my-contract :on-special-event {:some-param 1}]
                                               ["MyContract" :my-contract :on-special-event {:some-param 2}]
                                               ["MyContract" :my-contract :on-counter-incremented {:the-counter 8}]]
                              actual-events (atom [])]
                          (contracts/replay-past-events-in-order
                           [(contracts/create-event-filter :my-contract :on-special-event {} {:from-block 0 :to-block "latest"})
                            (contracts/create-event-filter :my-contract :on-counter-incremented {} {:from-block 0 :to-block "latest"})]
                           (fn [err evt]
                             (is (nil? err))
                             (let [args (map-vals (:args evt) #(js-invoke % "toNumber"))
                                   contract (:contract evt)]
                               (swap! actual-events #(into % [[(:name contract) (:contract-key contract) (:event evt) args]]))))
                           {:on-finish (fn []
                                         (is (= expected-events @actual-events))
                                         (done))}))))))))


(def forwarder-target-placeholder "beefbeefbeefbeefbeefbeefbeefbeefbeefbeef")


(deftest test-forwarders
  (testing "test-forwarders"

    (async done
           (-> (js/Promise.resolve true)
               #_(contracts/deploy-smart-contract! :my-contract [1])
               #_(.then #(contracts/deploy-smart-contract! :my-contract-fwd [] {:placeholder-replacements
                                                                                {forwarder-target-placeholder :my-contract}}))
               (.then #(-> (contracts/contract-call :my-contract-fwd :target)
                           (.then (fn [target] (is (= (contracts/contract-address :my-contract) target))))))

               (.then #(contracts/contract-call [:my-contract :my-contract-fwd] :set-counter [24]))
               (.then #(-> (contracts/contract-call [:my-contract :my-contract-fwd] :counter)
                           (.then (fn [counter] (is (= 24 (.toNumber counter)))))))

               ;; Direct way to use forwarders, assuming :forwards-to is defined in smart_contracts.cljs
               (.then #(contracts/contract-call :my-contract-fwd :set-counter [28]))
               (.then #(-> (contracts/contract-call :my-contract-fwd :counter)
                           (.then (fn [counter] (is (= 28 (.toNumber counter)))))))

               (.then #(contracts/contract-call :my-contract-fwd :set-target ["0xea674fdde714fd979de3edf0f56aa9716b898ec8"]
                                                {:ignore-forward? true}))

               (.then #(-> (contracts/contract-call :my-contract-fwd :target)
                           (.then (fn [target] (is (= "0xea674fdde714fd979de3edf0f56aa9716b898ec8" target))))))


               (.then #(contracts/contract-call :my-contract-fwd :set-target [(contracts/contract-address :my-contract)]
                                                {:ignore-forward? true}))

               (.then #(contracts/contract-call :my-contract-fwd :increment-counter [3]))

               (.then #(contracts/create-event-filter :my-contract-fwd :on-counter-incremented {} {:from-block 0 :to-block "latest"}
                                                      (fn [err {:keys [:contract :event]}]
                                                        (is (not err))
                                                        (is (= (:contract-key contract) :my-contract-fwd))
                                                        (is (= event :on-counter-incremented))
                                                        (done))))))))
