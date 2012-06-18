(ns shist.signatures
  (:import [org.apache.commons.codec.binary Hex])
  (:require [clojure.string :as str]
            [clojure.contrib.math :as math]))


(def random (java.security.SecureRandom.))

(def key-alphabet "abcdefghijklmnopqrstuvwxyz0123456789")
(def key-length 25)

(defn rand-char [chars]
  (let [len (.length chars)
        rand (math/abs (.nextInt random))]
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

(defn hmac [msg key]
  (let [keyspec (javax.crypto.spec.SecretKeySpec. (.getBytes key "UTF8") "HmacMD5")
        mac (doto (javax.crypto.Mac/getInstance "HmacMD5") (.init keyspec))]
    (Hex/encodeHexString (.doFinal mac (.getBytes msg "UTF8")))))

; No URL Encoding
(defn canonicalize [params]
  (str/join "&" (map #(str (name %1) "=" (%1 params)) (sort (keys params)))))

(defn signable-string [method path params]
  (str (if (nil? method) "XXX" (str/upper-case (name method)))
       " "
       path
       " "
       (canonicalize (dissoc params :signature :signature-valid))))

(defn sign [key method path params]
  (let [signable (signable-string method path params)]
    (hmac key signable)))
