(ns shist.core
  (:use compojure.core
        [ring.middleware.params :only [wrap-params]]
        [ring.middleware.keyword-params :only [wrap-keyword-params]])
  (:require [appengine-magic.core :as ae]))


(defroutes shist-app-routes
  (GET "/" req
       {:status 200
        :headers {"Content-Type" "text/plain"}
        :body "Hello, world! (updated)"})
  (GET "/test/:id" [id & params]
       (str "The ID is: " id " and param is: " (params :param)))
  )

; Makes GET parameters work in dev-appserver
; https://github.com/gcv/appengine-magic/issues/28
(def shist-app-handler
  (-> #'shist-app-routes
      wrap-keyword-params
      wrap-params))

(ae/def-appengine-app shist-app #'shist-app-handler)
