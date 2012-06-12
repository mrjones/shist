(ns shist.app_servlet
  (:gen-class :extends javax.servlet.http.HttpServlet)
  (:use shist.core)
  (:use [appengine-magic.servlet :only [make-servlet-service-method]]))


(defn -service [this request response]
  ((make-servlet-service-method shist-app) this request response))
