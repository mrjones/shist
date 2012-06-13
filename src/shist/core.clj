(ns shist.core
  (:import [java.security MessageDigest])
  (:use compojure.core
        [ring.middleware.params :only [wrap-params]]
        [ring.middleware.keyword-params :only [wrap-keyword-params]])
  (:require [appengine-magic.core :as ae]
            [appengine-magic.services.datastore :as ds]
            [clj-json.core :as json]))

;(ds/defentity KeyValuePair [^:key key, value])

(ds/defentity Command [ ^:key id, command, hostname, timestamp ])

(defn md5
  "Generate a md5 checksum for the given string"
  [token]
  (let [hash-bytes
        (doto (java.security.MessageDigest/getInstance "MD5")
          (.reset)
          (.update (.getBytes token)))]
    (.toString
     (new java.math.BigInteger 1 (.digest hash-bytes)) ; Positive and the size of the number
              16))) ; Use base16 i.e. hex

(defroutes shist-app-routes
  (GET "/" req
       {:status 200
        :headers {"Content-Type" "text/plain"}
        :body "Hello, world! (updated 4)"})
  ;; Insert a new command into the archive
  (POST "/commands/" [& params]
       (let [cmd (Command. (md5 (str (:host params) (:ts params)))
                           (:cmd params)
                           (:host params)
                           (:ts params))]
         (ds/save! cmd)
         (str "Timestamp: " (:timestamp cmd)
              " host: " (:hostname cmd)
              " cmd: " (:command cmd)
              " id: " (:id cmd))
         ))
  (GET "/command/:cmdid" [cmdid]
       (let [cmd (ds/retrieve Command cmdid)]
         (if (nil? cmd)
           {:status 404 :body (str cmdid " not found sir.")}
           {:status 200 :body (json/generate-string cmd)})))
;  (GET "/store/:key/:value" [key value]
;       ; Do a lookup (check for dupes) first
;       (let [kv (KeyValuePair. "foo" value)]
;         (ds/save! kv)
;         (str "Setting " key " to " value ". P.S. " (:key kv) (:value kv))))
;  (GET "/lookup/:key" [key]
;       ; Figure out how to construct a key to make ds/retrieve work
;       (let [kv (first (ds/query :kind KeyValuePair :filter (= :key key)))]
;         (if (nil? kv)
;           (str "Couldn't find " key)
;           (str "Looking up " key ". Got " (:value kv)))))
  (GET "/favicon.ico" [] { :status 404 })
  )

; Right now you need to make sure the header:
; Content-Type:application/x-www-form-urlencoded
; I think we might be able to omit that with something like
; (defn wrap-correct-content-type [handler]
;   (fn [request]
;      (handler (assoc request :content-type "application/json"))))

; Makes GET parameters work in dev-appserver
; https://github.com/gcv/appengine-magic/issues/28
(def shist-app-handler
  (-> #'shist-app-routes
      wrap-keyword-params
      wrap-params))

(ae/def-appengine-app shist-app #'shist-app-handler)
