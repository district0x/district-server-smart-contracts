(ns tests.runner
  (:require [cljs.nodejs :as nodejs]
            [tests.async]
            [doo.runner :refer-macros [doo-tests]]))

(nodejs/enable-util-print!)

(doo-tests
 'tests.async)
