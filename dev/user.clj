(ns user
  (:require [figwheel-sidecar.repl-api]))

(defn start-tests! []
  (figwheel-sidecar.repl-api/start-figwheel! (figwheel-sidecar.config/fetch-config) "tests")
  (figwheel-sidecar.repl-api/cljs-repl "tests"))

(comment
  (start-tests!))
