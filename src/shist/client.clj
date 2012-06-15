(ns shist.client
  (:require [clj-http.client :as http]))

(defn hitroot []
  (http/get "http://localhost:8081"))
  
