(ns tests.sync
  (:require [cljs.test :refer-macros [deftest use-fixtures]]
            [mount.core :as mount]
            [tests.smart-contracts]
            [tests.suite :refer [test-smart-contracts]]))

(use-fixtures
  :once
  {:before (fn []
             (-> (mount/with-args
                   {:web3 {:port 8549}
                    :smart-contracts {:contracts-var #'tests.smart-contracts/smart-contracts
                                      :print-gas-usage? true
                                      :auto-mining? false}})
                 (mount/start)))
   :after (fn []
            (mount/stop))})

(deftest test-smart-contracts-sync
  (test-smart-contracts))
