(ns shist.client
  (:require [clj-http.client :as http]))

(defrecord Target [hostname port])

(def localserver (Target. "localhost" 8081))

(defn serverstr [target path]
  (str "http://" (:hostname target) ":" (:port target) path))

(defn listcommands [target]
  (http/get (serverstr target "/commands/")))

(defn getcommand [target md5]
  (http/get (serverstr target (str "/command/" md5))))

(defn addcommand [target cmdhost cmdts cmdtty cmdbody cmdowner]
  (http/post (serverstr target "/commands/")
             {:body (http/generate-query-string
                     (hash-map :cmd cmdbody
                               :host cmdhost
                               :ts cmdts
                               :tty cmdtty
                               :owner cmdowner))
              :content-type "application/x-www-form-urlencoded"}))


