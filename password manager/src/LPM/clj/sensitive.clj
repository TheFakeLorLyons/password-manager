(ns LPM.clj.sensitive
  (:require [buddy.core.crypto :as crypto]
            [buddy.hashers :as hashers]))

(def secret-key "my-secret-key") ; Use a secure key

(defn encrypt [data]
  (crypto/encrypt secret-key data))

(defn decrypt [encrypted-data]
  (crypto/decrypt secret-key encrypted-data))

(defn hash-password [login-password]
  (hashers/derive login-password))

(defn verify-password [login-password hashed-password]
  (hashers/check login-password hashed-password))