(ns shist.core
  (:use compojure.core
        [ring.middleware.params :only [wrap-params]]
        [ring.middleware.keyword-params :only [wrap-keyword-params]])
  (:require [appengine-magic.core :as ae]
            [appengine-magic.services.datastore :as ds]))

(ds/defentity KeyValuePair [^:key key, value])

(defroutes shist-app-routes
  (GET "/" req
       {:status 200
        :headers {"Content-Type" "text/plain"}
        :body "Hello, world! (updated 4)"})
  (GET "/store/:key/:value" [key value]
       ; Do a lookup (check for dupes) first
       (let [kv (KeyValuePair. "foo" value)]
         (ds/save! kv)
         (str "Setting " key " to " value ". P.S. " (:key kv) (:value kv))))
  (GET "/lookup/:key" [key] #".*"
       ; Figure out how to construct a key to make ds/retrieve work
       (let [kv (first (ds/query :kind KeyValuePair :filter (= :key key)))]
         (if (nil? kv)
           (str "Couldn't find " key)
           (str "Looking up " key ". Got " (:value kv)))))
  (GET "/favicon.ico" [] { :status 404 })
  )

; Makes GET parameters work in dev-appserver
; https://github.com/gcv/appengine-magic/issues/28
(def shist-app-handler
  (-> #'shist-app-routes
      wrap-keyword-params
      wrap-params))

(ae/def-appengine-app shist-app #'shist-app-handler)
