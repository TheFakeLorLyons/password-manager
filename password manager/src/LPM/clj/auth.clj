(ns LPM.clj.auth
(:require [buddy.auth :as auth]
          [buddy.auth.backends :as backends]
          [buddy.auth.middleware :refer [wrap-authentication]]
          [buddy.hashers :as hashers]
          [ring.adapter.jetty :as jetty]
          [LPM.clj.sensitive :as sns]))

(defn hash-password [login-password]
  (hashers/derive login-password {:alg :argon2id}))

(defn verify-password [login-password hashed-password]
  (hashers/verify login-password hashed-password))

(defn verify-hashed-password [password hashed-password]
  (try
    (hashers/verify password hashed-password)
    true
    (catch Exception _
      false)))

#_(defn register-new-user [username password secret-key]
  (let [hashed-password (hash-password password)
        encrypted-password (sns/encrypt-entry hashed-password secret-key)]
    (save-user-to-db username encrypted-password)))

(defn authenticate [entered-password hashed-password]
  (try
    {:authenticated (hashers/check entered-password hashed-password)}
    (catch Exception e
      (println "Authentication error:" (.getMessage e))
      {:authenticated false})))

(def auth-backend (backends/basic {:realm "Lastword"
                                   :authfn authenticate}))
