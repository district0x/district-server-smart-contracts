(ns tests.async
  (:require [cljs.test :refer-macros [deftest is testing use-fixtures]]
            [district.server.smart-contracts :as contracts]
            [mount.core :as mount]
            [tests.smart-contracts]))

(use-fixtures
  :once
  {:before (fn []
             (-> (mount/with-args
                   {:web3 {:port 8548}
                    :smart-contracts {:contracts-var #'tests.smart-contracts/smart-contracts
                                      :print-gas-usage? true
                                      :auto-mining? true}})
                 (mount/start)))
   :after (fn []
            (mount/stop))})

(deftest test-smart-contracts-async
  (is (false? (empty? (contracts/contract-abi :my-contract))))
  (is (false? (empty? (contracts/contract-bin :my-contract))))
  (is (= (contracts/contract-address :my-contract) "0x0000000000000000000000000000000000000000"))
  (is (= (contracts/contract-name :my-contract) "MyContract"))

  (is (map? (contracts/deploy-smart-contract! :my-contract {:arguments [1]})))

  (is (not= (contracts/contract-address :my-contract) "0x0000000000000000000000000000000000000000"))

  (is (= 1 (.toNumber (contracts/contract-call :my-contract :counter))))

  (is (= 5 (.toNumber (contracts/contract-call :my-contract :my-plus 2 3))))

  (let [tx-hash (contracts/contract-call :my-contract :increment-counter 2 {:gas 500000})]
    (is (string? tx-hash))
    (let [{:keys [:args]} (contracts/contract-event-in-tx tx-hash :my-contract :on-counter-incremented)]
      (is (= 3 (.toNumber (:the-counter args))))))

  (let [tx-hash (contracts/contract-call :my-contract :double-increment-counter 2 {:gas 500000})]
    (is (string? tx-hash))
    (is (= (map (comp #(.toNumber %) :the-counter :args)
                (contracts/contract-events-in-tx tx-hash :my-contract :on-counter-incremented))
           [5 7])))

  ;; corner case, function which returns address
  (is (= "0xbeefbeefbeefbeefbeefbeefbeefbeefbeefbeef" (contracts/contract-call :my-contract :target)))

  (is (= 7 (.toNumber (contracts/contract-call :my-contract :counter)))))