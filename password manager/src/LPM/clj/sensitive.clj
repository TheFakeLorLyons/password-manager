(ns LPM.clj.sensitive
  (:require [buddy.hashers :as hashers]
            [clojure.string :as str])
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

(defn hex->bytes [hex-string]
  (let [len (count hex-string)
        bytes (byte-array (/ len 2))]
    (doseq [i (range 0 len 2)]
      (aset bytes (/ i 2)
            (byte (Integer/parseInt (subs hex-string i (+ i 2)) 16))))
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
       iv (generate-random-iv iv-size)
       gcm-spec (GCMParameterSpec. tag-size iv)
       key-spec (SecretKeySpec. secret-key "AES")]

   (println "IV generated (hex):" (bytes->hex iv))

   (try
     (.init cipher Cipher/ENCRYPT_MODE key-spec gcm-spec)
     (let [data-bytes (.getBytes data "UTF-8")
           encrypted (.doFinal cipher data-bytes)
           result (byte-array (concat iv encrypted))]
       (println "Encrypted content (base64):" (bytes->base64 result))
       (bytes->base64 result)) ; Convert to base64 for storage
     (catch Exception e
       (println "Encryption error:" (.getMessage e))
       nil))))

(defn decrypt-entry [encrypted-data secret-key]
  (println "Inside sense" encrypted-data "AND SECRET KEY: " secret-key)
  (println "Well!? " (is-valid-aes-key? (bytes->base64 secret-key)))
  (println "Read Enc key (base64):" (bytes->base64 secret-key))
  (let [cipher (Cipher/getInstance "AES/GCM/NoPadding" "BC")
       all-bytes (base64->bytes encrypted-data)
       iv (byte-array iv-size)
       encrypted (byte-array (- (count all-bytes) iv-size))]

   (System/arraycopy all-bytes 0 iv 0 iv-size)
   (System/arraycopy all-bytes iv-size encrypted 0 (count encrypted))

   (println "IV (hex):" (bytes->hex iv))
   (println "Encrypted data (hex):" (bytes->hex encrypted))

   (let [gcm-spec (GCMParameterSpec. tag-size iv)
         key-spec (SecretKeySpec. secret-key "AES")]
     (try
       (.init cipher Cipher/DECRYPT_MODE key-spec gcm-spec)
       (let [decrypted-bytes (.doFinal cipher encrypted)]
         (println "Decrypted bytes (hex):" (bytes->hex decrypted-bytes))
         (String. decrypted-bytes "UTF-8"))
       (catch Exception e
         (println "Decryption error:" (.getMessage e))
         (.printStackTrace e)
         nil)))))

(defn hash-password [login-password]
  (hashers/derive login-password {:alg :argon2id}))

(defn verify-password [login-password hashed-password]
  (hashers/verify login-password hashed-password))