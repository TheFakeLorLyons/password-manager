(ns LPM.clj.sensitive
  (:require [buddy.hashers :as hashers])
  (:import [org.bouncycastle.jce.provider BouncyCastleProvider]
           [javax.crypto Cipher]
           [javax.crypto.spec SecretKeySpec GCMParameterSpec]
           [java.util Base64]
           [java.security SecureRandom Security]))

(def ^:private key-size 256)
(def ^:private iv-size 12)
(def ^:private tag-size 128)

(Security/addProvider (BouncyCastleProvider.))

(defn generate-random-iv [size]
  (let [iv (byte-array size)]
    (.nextBytes (SecureRandom.) iv)
    iv))

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

(defn base64->bytes [base64-str]
  (.decode (Base64/getDecoder) base64-str))

(defn bytes->base64 [bytes]
  (.encodeToString (Base64/getEncoder) bytes))

(defn is-valid-aes-key? [base64-key]
  (let [key-bytes (base64->bytes base64-key)]
    (or (= (count key-bytes) 16)  ; 128 bits
        (= (count key-bytes) 24)  ; 192 bits
        (= (count key-bytes) 32)))) ; 256 bits


(defn encrypt [data secret-key]
  (println "Inside sense" data)
  (println "Well? " (is-valid-aes-key? (bytes->base64 secret-key)))
  (println "Secret key (base64):" (bytes->base64 secret-key))
  (let [cipher (Cipher/getInstance "AES/GCM/NoPadding" "BC")
        _ (println "Cipher instance created" cipher)
        iv (byte-array iv-size)
        _ (.nextBytes (SecureRandom.) iv)
        _ (println "IV generated" iv)
        gcm-spec (GCMParameterSpec. tag-size iv)
        _ (println "GCMParameterSpec created" gcm-spec)
        key-spec (SecretKeySpec. secret-key "AES")
        _ (println "key-spec created" key-spec)]
    (println "csv content in sensitive" key-spec)
    (try
      (println "try")
      (.init cipher Cipher/ENCRYPT_MODE key-spec gcm-spec)
      (let [data-bytes (.getBytes data "UTF-8")
            encrypted (.doFinal cipher data-bytes)]
        (println "Encrypted content:" (bytes->base64 encrypted))
        (let [result (byte-array (concat iv encrypted))]
          (println "Result byte array:" (bytes->base64 result))
          (bytes->hex result)))
      (catch Exception e
        (println "Encryption error:" (.getMessage e))
        nil))))

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