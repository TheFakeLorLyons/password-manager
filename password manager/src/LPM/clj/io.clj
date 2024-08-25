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

(defn read-encrypted-csv [csv-data]
  (let [keys (sup/load-keys)
        secret-key (:secret-key keys)
        csv-content (csv/read-csv csv-data)
        [user-info & passwords] csv-content
        [username encrypted-password] user-info
        decrypted-user {:userProfileName username
                        :userLoginPassword (sns/decrypt encrypted-password secret-key)}
        decrypted-passwords (for [[name encrypted-content encrypted-notes] passwords]
                              {"pName" name
                               "pContent" (sns/decrypt encrypted-content secret-key)
                               "pNotes" (sns/decrypt encrypted-notes secret-key)})]
    (println "keys in handles" keys)
    (println "csv content in handles" csv-content)
    (println "user-info content in handles" user-info)
    (println "pws: " decrypted-user)
    (println "data in handles" decrypted-passwords) 
    {:profile (assoc decrypted-user :passwords decrypted-passwords)}))