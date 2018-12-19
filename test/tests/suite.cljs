(ns tests.suite
  (:require [cljs.test :refer-macros [is testing async]]
            [district.server.smart-contracts :as contracts]))

(defn test-smart-contracts []
  (testing "smart contracts test suite"

    (is (false? (empty? (contracts/contract-abi :my-contract))))
    (is (false? (empty? (contracts/contract-bin :my-contract))))

    (is (= (contracts/contract-address :my-contract) "0x0000000000000000000000000000000000000000"))
    (is (= (contracts/contract-name :my-contract) "MyContract"))

    (async done
           (-> (contracts/deploy-smart-contract! :my-contract [1])

               (.then #(contracts/create-event-filter :my-contract :on-counter-incremented {} "latest" (fn []
                                                                                                         (is (= "MyContract" (:name %))))))

               (.then #(is (not= (contracts/contract-address :my-contract) "0x0000000000000000000000000000000000000000")))

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
                                    (is (= 7 (.toNumber counter)))
                                    (done)))))))))
