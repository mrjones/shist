(ns shist.core
  (:use compojure.core)
  (:require [appengine-magic.core :as ae]))


;(defn shist-app-handler [request]
;  {:status 200
;   :headers {"Content-Type" "text/plain"}
;   :body "Hello, world!"})

(defroutes shist-app-handler
  (GET "/" req
       {:status 200
        :headers {"Content-Type" "text/plain"}
        :body "Hello, world!"})
  )

(ae/def-appengine-app shist-app #'shist-app-handler)
