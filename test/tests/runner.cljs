(ns tests.runner
  (:require
    [cljs.nodejs :as nodejs]
    [cljs.test :refer [run-tests]]
    [tests.all]))

(nodejs/enable-util-print!)

(defn -main [& _]
  (run-tests 'tests.all))

(set! *main-cli-fn* -main)