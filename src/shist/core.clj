(ns shist.core
  (:require [appengine-magic.core :as ae]))


(defn shist-app-handler [request]
  {:status 200
   :headers {"Content-Type" "text/plain"}
   :body "Hello, world!"})


(ae/def-appengine-app shist-app #'shist-app-handler)