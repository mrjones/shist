(ns shist.client
  (:use shist.signatures)
  (:require [clj-http.client :as http]))

(def local "http://localhost:8081")
(def appengine-http "http://shellhistory.appspot.com/")
(def appengine-https "https://shellhistory.appspot.com/")


(defn signed-get [host path param-map key]
  (let [hmac (sign key "GET" path param-map)
        signed-param-map (assoc param-map :signature hmac)]
    (println (str host path "?" (http/generate-query-string signed-param-map)))
    (http/get (str host path "?" (http/generate-query-string signed-param-map))
              {:throw-exceptions false})))

(defn signed-post [host path query-params body-params key]
  (let [
        hmac (sign key "POST" path (merge query-params body-params))
        signed-params (assoc query-params :signature hmac)
        body (http/generate-query-string body-params)]
    (println (str host path "?" (http/generate-query-string signed-params) " / " body))
    (http/post (str host path "?" (http/generate-query-string signed-params))
;               {:body body :content-type "application/x-www-form-urlencoded"})))
               {:body body
                :content-type "application/x-www-form-urlencoded"
                :throw-exceptions false})))

(defn listcommands [target-host key]
  (signed-get target-host "/commands/" {} key))

(defn getcommand [target-host md5 key]
  (signed-get target-host (str "/command/" md5) {} key))

(defn addcommand [target-host cmdhost cmdts cmdtty cmdbody cmdowner key]
  (let [body-map {:cmd cmdbody :host cmdhost :ts cmdts :tty cmdtty :owner cmdowner}]
    (signed-post target-host "/commands/" {} body-map key)))

;(defn addcommand [target cmdhost cmdts cmdtty cmdbody cmdowner]
;  (http/post (str target "/commands/")
;             {:body (http/generate-query-string
;                     (hash-map :cmd cmdbody
;                               :host cmdhost
;                               :ts cmdts
;                               :tty cmdtty
;                               :owner cmdowner))
;              :content-type "application/x-www-form-urlencoded"}))


