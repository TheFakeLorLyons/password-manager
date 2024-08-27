(ns LPM.clj.user
  (:require [buddy.hashers :as hsh]))

(def current-user (atom
                   {:users
                    {"profile" {:userProfileName nil
                                :userLoginPassword nil
                                :passwords [{:pName nil
                                             :pContent nil
                                             :pNotes nil}]}}}))

(defn create-account [profile-name login-password]
  (let [hashed-password (hsh/derive login-password {:alg :argon2id})
        user-profile {:userProfileName profile-name
                      :userLoginPassword hashed-password
                      :passwords []}]
    (swap! current-user
           assoc-in [:users profile-name] user-profile)
    user-profile))

