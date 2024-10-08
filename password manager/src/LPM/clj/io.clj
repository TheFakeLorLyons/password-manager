(ns LPM.clj.io
  (:require [clojure.data.csv :as csv]
            [clojure.string :as str]
            [LPM.clj.setup :as sup]
            [LPM.clj.sensitive :as sns]
            [LPM.clj.auth :as auth]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
                                        ;     converting csv data to atom     ;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn csv-to-current-user [csv-string]
  (try
    (let [lines (str/split-lines csv-string)
          [header & password-lines] lines
          [profile-name login-password] (str/split header #",")]
      {:userProfileName profile-name
       :userLoginPassword login-password
       :passwords (mapv (fn [line]
                          (let [[pName pContent pNotes] (str/split line #",")]
                            {:pName pName
                             :pContent pContent
                             :pNotes pNotes}))
                        password-lines)})
    (catch Exception e
      (println "Error processing CSV:" (.getMessage e))
      nil)))

(defn generate-csv [current-user]
  (let [profile (get-in current-user [:users "profile"])
        user-info [(get profile :userProfileName) (get profile :userLoginPassword)]
        passwords (get profile :passwords)
        data (for [password passwords]
               [(get password "pName")
                (get password "pContent")
                (get password "pNotes")])
        csv-data (cons user-info data)]
    (with-out-str
      (csv/write-csv *out* csv-data))))

(defn generate-encrypted-csv [current-user]
  (let [keys (sup/load-keys)
        secret-key (:secret-key keys)
        user-info [(get current-user "userProfileName")
                   (get current-user "userLoginPassword")]
        passwords (get current-user "passwords")
        data (for [password passwords]
               [(get password "pName")
                (sns/encrypt (get password "pContent") secret-key)
                (sns/encrypt (get password "pNotes") secret-key)])
        csv-data (cons user-info data)] 
    (with-out-str
      (csv/write-csv *out* csv-data))))

(defn parse-encrypted-data [data-string]
  (let [entries (str/split data-string #"\n")]
    (into {} (map (fn [entry]
                    (let [[label data] (str/split entry #",")]
                      [label data]))
                  entries))))

(defn read-encrypted-csv [csv-data]
  (let [csv-content (get csv-data "csv-content")
        keys (sup/load-keys) 
        secret-key (:secret-key keys) 
        [user-info & passwords] (str/split csv-content #"\n") 
        [username existing-hashed-password] (str/split user-info #",")
        auth-result (auth/authenticate (get csv-data "userLoginPassword") existing-hashed-password)]
    (if (:authenticated auth-result)
      (let [decrypted-user {:userProfileName username
                            :userLoginPassword existing-hashed-password}
            decrypted-passwords (for [password-line passwords
                                      :let [[name encrypted-content encrypted-notes] (str/split password-line #",")]]
                                  {:pName name
                                   :pContent (sns/decrypt-entry encrypted-content secret-key)
                                   :pNotes (sns/decrypt-entry encrypted-notes secret-key)})]
        {:authenticated true
         :userProfileName (:userProfileName decrypted-user)
         :userLoginPassword (:userLoginPassword decrypted-user)
         :passwords decrypted-passwords})
      {:authenticated false})))