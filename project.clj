(defproject shist "1"
  :description "FIXME: write description"
  :aot [shist.app_servlet]
  :dependencies [
                 [clj-http "0.1.1"]
                 [clj-json "0.5.0"]
                 [commons-codec/commons-codec "1.4"]
                 [compojure "1.1.0"]
                 [org.clojure/clojure "1.3.0"]
                 [hiccup "1.0.0"]
                 ]
  :dev-dependencies [
                     [appengine-magic "0.5.0"]
                     [swank-clojure "1.4.0-SNAPSHOT"]
                     ]
  )
