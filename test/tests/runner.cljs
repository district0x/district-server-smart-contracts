(ns tests.runner
  (:require [cljs.nodejs :as nodejs]
            [tests.async]
            [tests.sync]
            [doo.runner :refer-macros [doo-tests]]))

(nodejs/enable-util-print!)

(doo-tests
 'tests.async
 'tests.sync)
