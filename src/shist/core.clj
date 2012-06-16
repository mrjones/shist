(ns shist.core
  (:import [java.security MessageDigest]
           [org.apache.commons.codec.binary Hex])
  (:use compojure.core
        hiccup.core
        [clojure.contrib.string :only [substring?]]
        [clojure.contrib.math :only [abs]]
        [ring.middleware.params :only [wrap-params]]
        [ring.middleware.keyword-params :only [wrap-keyword-params]])
  (:require [appengine-magic.core :as ae]
            [appengine-magic.services.datastore :as ds]
            [appengine-magic.services.user :as user]
            [clj-json.core :as json]))

(ds/defentity Command [ ^:key id, command, hostname, timestamp, tty, owner ])

(ds/defentity ApiKey [ ^:key key, owner ])

(def key-alphabet "abcdefghijklmnopqrstuvwxyz0123456789")
(def key-length 25)

(def random (java.security.SecureRandom.))

(defn rand-char [chars]
  (let [len (.length chars)
        rand (abs (.nextInt random))]
    (.charAt chars (rem rand len))
    ))

(defn rand-chars [accum n valid-chars]
  (if (= n 0)
    accum
    (rand-chars (str accum (rand-char valid-chars)) (- n 1) valid-chars)))

(defn gen-key []
  ; TODO(mrjones): replace with
  ; KeyGenerator kg = KeyGenerator.getInstance("HmacMD5");
  ; SecretKey sk = kg.generateKey();
  (rand-chars "" key-length key-alphabet))

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

(defn hmac [msg key]
  (let [keyspec (javax.crypto.spec.SecretKeySpec. (.getBytes key "UTF8") "HmacMD5")
        mac (doto (javax.crypto.Mac/getInstance "HmacMD5") (.init keyspec))]
    (Hex/encodeHexString (.doFinal mac (.getBytes msg "UTF8")))))

(defn manage-keys-ui []
  (html
   [:div (str "Logged in as " (user/current-user))]
   [:div [:a {:href "/add_key"} "Add Key" ]]
   (for [x (ds/query :kind ApiKey :filter (= :owner (user/current-user))) ]
     [:div (str "Key: [" (:key x) "] [" (hmac "foobar" (:key x)) "]" )])
   ))


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
         (json/generate-string filtercmds)))
  
  ;; List one command
  (GET "/command/:cmdid" [cmdid]
       (let [cmd (ds/retrieve Command cmdid)]
         (if (nil? cmd)
           {:status 404 :body (str cmdid " not found sir.")}
           {:status 200 :body (json/generate-string cmd)})))

  ;; Non-REST interactive (key-management) UI
  
  (GET "/manage_keys*", []
       (if (user/user-logged-in?)
         (manage-keys-ui)
         {:status 302
          :headers {"Location" (user/login-url :destination "/manage_keys")}
          :body ""}))

  (GET "/add_key", []
       (let [key (ApiKey. (gen-key) (user/current-user))]
         (ds/save! key)
         {:status 302
          :headers {"Location" "/manage_keys"}
          :body ""}))

  (GET "/logout", []
       {:status 302
        :headers {"Location" (user/logout-url)}
        :body ""})

  ;; Chrome always asks for a favicon. This suppresses error traces
  (GET "/favicon.ico" [] { :status 404 })

  (ANY "*" [] { :status 404 :body "404 NOT FOUND"})

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
