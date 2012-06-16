(ns shist.client
  (:use shist.signatures)
  (:require [clj-http.client :as http]))

(def local "http://localhost:8081")
(def appengine-http "http://shellhistory.appspot.com/")
(def appengine-https "https://shellhistory.appspot.com/")

(defn listcommands [target filter]
  (http/get (str target "/commands/" filter)))

(defn signed-get [host path param-map key]
  (let [hmac (sign key "GET" path param-map "")
        signed-param-map (assoc param-map :signature hmac)]
    (println (str host path "?" (http/generate-query-string signed-param-map)))
    (http/get (str host path "?" (http/generate-query-string signed-param-map)))))
    

(defn getcommand [target md5]
  (http/get (str target "/command/" md5)))

(defn addcommand [target cmdhost cmdts cmdtty cmdbody cmdowner]
  (http/post (str target "/commands/")
             {:body (http/generate-query-string
                     (hash-map :cmd cmdbody
                               :host cmdhost
                               :ts cmdts
                               :tty cmdtty
                               :owner cmdowner))
              :content-type "application/x-www-form-urlencoded"}))


