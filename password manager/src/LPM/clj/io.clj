(ns LPM.clj.io
  (:require [clojure.data.csv :as csv]
            [clojure.string :as str]
            [LPM.clj.setup :as sup]
            [LPM.clj.sensitive :as sns]))

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
  (println "in IO:" current-user)
  (let [keys (sup/load-keys)
        secret-key (:secret-key keys)
        _ (println "Secret key (base64) in gen-encry-key:" secret-key)
        user-info [(get current-user "userProfileName")
                   (sns/encrypt (get current-user "userLoginPassword") secret-key)]
        _ (println "user-info):" user-info)
        passwords (get current-user "passwords")
        _ (println "passwords:" passwords)
        data (for [password passwords]
               [(get password "pName")
                (sns/encrypt (get password "pContent") secret-key)
                (sns/encrypt (get password "pNotes") secret-key)])
        _ (println "Secret key (base64):" data)
        csv-data (cons user-info data)] 
    _ (println "csv-data:" csv-data)
    (println "Secret key (base64) in gen-encry-key:" secret-key)
    (with-out-str
      (csv/write-csv *out* csv-data))))

(defn parse-encrypted-data [data-string]
  (let [entries (str/split data-string #"\n")]
    (into {} (map (fn [entry]
                    (let [[label data] (str/split entry #",")]
                      [label data]))
                  entries))))

(defn read-encrypted-csv [csv-data]
  (println "csv data " csv-data)
  (let [csv-content (get csv-data "csv-content")
        _ (println "csv data " csv-content)
        keys (sup/load-keys)
        _ (println "keys1 csv data " keys)
        secret-key (:secret-key keys)
        _ (println "secret-key " secret-key)
        [user-info & passwords] (str/split csv-content #"\n")
        _ (println "user CONTENT IO " user-info)
        [username encrypted-password] (str/split user-info #",")
        _ (println "user info in read-enc " username "pw: " encrypted-password)
        decrypted-user {:userProfileName csv-data
                        :userLoginPassword (sns/decrypt-entry encrypted-password secret-key)}
        _ (println "user decrypted-user IO " decrypted-user)
        decrypted-passwords (for [password-line passwords
                                  :let [[name encrypted-content encrypted-notes] (str/split password-line #",")]]
                              {:pName name
                               :pContent (sns/decrypt-entry encrypted-content secret-key)
                               :pNotes (sns/decrypt-entry encrypted-notes secret-key)})]
    (println "DECRYPTED PWS! " decrypted-passwords)
    {:userProfileName (:userProfileName decrypted-user)
     :userLoginPassword (:userLoginPassword decrypted-user)
     :passwords decrypted-passwords}))