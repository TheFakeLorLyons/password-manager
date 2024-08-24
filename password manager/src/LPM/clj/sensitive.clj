(ns LPM.clj.sensitive
  (:require [buddy.hashers :as hashers])
  (:import [javax.crypto Cipher]
           [javax.crypto.spec SecretKeySpec GCMParameterSpec]
           [java.util Base64]
           [java.security SecureRandom]))

(def ^:private key-size 256)
(def ^:private iv-size 12)
(def ^:private tag-size 128)

(defn- hex->bytes [hex-string]
  (let [bytes (byte-array (/ (count hex-string) 2))]
    (dotimes [i (count bytes)]
      (aset-byte bytes i (Integer/parseInt (subs hex-string (* 2 i) (* 2 (+ i 1))) 16)))
    bytes))

(defn- bytes->hex [bytes]
  (apply str
         (map (fn [b] (format "%02x" b))
              bytes)))
(defn- get-secret-key []
  (let [key (System/getenv "SECRET_KEY")]
    (if key
      (hex->bytes key)
      (throw (Exception. "SECRET_KEY environment variable not set")))))

(def secret-key (delay (get-secret-key)))

(defn encrypt [data secret-key]
  (let [cipher (Cipher/getInstance "AES/GCM/NoPadding")
        iv (byte-array iv-size)
        _ (.nextBytes (SecureRandom.) iv)
        gcm-spec (GCMParameterSpec. tag-size iv)
        key-spec (SecretKeySpec. (hex->bytes secret-key) "AES")]
    (.init cipher Cipher/ENCRYPT_MODE key-spec gcm-spec)
    (let [encrypted (.doFinal cipher (.getBytes data "UTF-8"))]
      (bytes->hex (byte-array (concat iv encrypted))))))

(defn decrypt [encrypted-data secret-key]
  (let [cipher (Cipher/getInstance "AES/GCM/NoPadding")
        all-bytes (hex->bytes encrypted-data)
        iv (byte-array (take iv-size all-bytes))
        encrypted (byte-array (drop iv-size all-bytes))
        gcm-spec (GCMParameterSpec. tag-size iv)
        key-spec (SecretKeySpec. (hex->bytes secret-key) "AES")]
    (.init cipher Cipher/DECRYPT_MODE key-spec gcm-spec)
    (String. (.doFinal cipher encrypted) "UTF-8")))

(defn hash-password [login-password]
  (hashers/derive login-password {:alg :argon2id}))

(defn verify-password [login-password hashed-password]
  (hashers/verify login-password hashed-password))