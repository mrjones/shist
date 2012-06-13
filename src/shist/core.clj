(ns shist.core
  (:import [java.security MessageDigest])
  (:use compojure.core
        [ring.middleware.params :only [wrap-params]]
        [ring.middleware.keyword-params :only [wrap-keyword-params]])
  (:require [appengine-magic.core :as ae]
            [appengine-magic.services.datastore :as ds]
            [clj-json.core :as json]))

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
  ;; Make this POST-only
  (ANY "/commands/" [& params]
       (let [cmd (Command. (md5 (str (:host params) (:ts params)))
                           (:cmd params)
                           (:host params)
                           (:ts params))]
         (str "Timestamp: " (:timestamp cmd)
              " host: " (:hostname cmd)
              " cmd: " (:command cmd)
              " id: " (:id cmd))
;;         (ds/save! cmd)
         ))
  (GET "/command/:cmdid" [cmdid]
       (let [cmd (ds/retrieve Command cmdid)]
         (if (nil? cmd)
           {:status 404 :body (str cmdid " not found")}
           {:status 200 :body (json/generate-string cmd)})))
  (GET "/favicon.ico" [] { :status 404 })
  )


; Makes GET parameters work in dev-appserver
; https://github.com/gcv/appengine-magic/issues/28
(def shist-app-handler
  (-> #'shist-app-routes
      wrap-keyword-params
      wrap-params))

(ae/def-appengine-app shist-app #'shist-app-handler)
