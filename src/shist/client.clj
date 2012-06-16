(ns shist.client
  (:require [clj-http.client :as http]))

(def local "http://localhost:8081")
(def appengine-http "http://shellhistory.appspot.com/")
(def appengine-https "https://shellhistory.appspot.com/")

(defn listcommands [target filter]
  (http/get (str target (str "/commands/" filter))))

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


