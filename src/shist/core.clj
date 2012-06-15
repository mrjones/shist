(ns shist.core
  (:import [java.security MessageDigest])
  (:use compojure.core
        [clojure.contrib.string :only [substring?]]
        [ring.middleware.params :only [wrap-params]]
        [ring.middleware.keyword-params :only [wrap-keyword-params]])
  (:require [appengine-magic.core :as ae]
            [appengine-magic.services.datastore :as ds]
            [clj-json.core :as json]))

(ds/defentity Command [ ^:key id, command, hostname, timestamp, tty, owner ])

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

(defn parselong [s] (. Long parseLong s))
(def maxlong (. Long MAX_VALUE))

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
                           (parselong (:ts params))
                           (:tty params)
                           (:owner params))]
         (ds/save! cmd)
         (str "Timestamp: " (:timestamp cmd)
              " host: " (:hostname cmd)
              " cmd: " (:command cmd)
              " id: " (:id cmd))
         ))

  ;; List all commands
  (GET "/commands/" [& params]
       (let [mints (if (nil? (:mints params)) 0 (parselong (:mints params)))
             maxts (if (nil? (:maxts params)) maxlong (parselong (:maxts params)))
             cmdfilter (if (nil? (:filter params)) "" (:filter params))
             cmds (ds/query :kind Command :filter
                            [(>= :timestamp mints) (<= :timestamp maxts)])
             filtercmds (filter #(substring? cmdfilter (:command %)) cmds)
             ]
         (str cmdfilter "->" (json/generate-string filtercmds))))
  
  ;; List one command
  (GET "/command/:cmdid" [cmdid]
       (let [cmd (ds/retrieve Command cmdid)]
         (if (nil? cmd)
           {:status 404 :body (str cmdid " not found sir.")}
           {:status 200 :body (json/generate-string cmd)})))

  ;; Chrome always asks for a favicon. This suppresses error traces
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
