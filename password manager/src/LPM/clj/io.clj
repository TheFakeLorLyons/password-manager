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
      (throw (ex-info (str "Error processing CSV:" (ex-message e))
                      {:id ::csv-failed
                       :csv-string csv-string}
                      e)))))

(defn extract-user-data [data]
  (let [{:keys [csv-content userProfileName userLoginPassword]} data
        user-data  (csv-to-current-user csv-content)
        pw-to-compare (:userLoginPassword user-data)
        username-to-compare (:userProfileName user-data)]
    (when (and (= userProfileName username-to-compare)
               (auth/authenticate (auth/hash-password userLoginPassword) pw-to-compare))
      user-data)))

(defn generate-csv [current-user]
  (let [{:keys [userProfileName userLoginPassword passwords]} current-user
        password-list (for [{:keys [pName pContent pNotes]} passwords]
               [pName pContent pNotes]) 
        csv-data (cons [userProfileName userLoginPassword] password-list)]
    (with-out-str
      (csv/write-csv *out* csv-data))))

(defn generate-encrypted-csv [current-user]
  (let [keys (sup/load-keys)
        _ (println "Loaded -keys: " keys)
        secret-key (:secret-key keys) 
        _ (println "Loaded secret-key: " secret-key)
        {:keys [userProfileName userLoginPassword passwords]} current-user
        _ (println "cu enc data: " userProfileName "pws " passwords)
        password-list (for [{:keys [pName pContent pNotes]} passwords]
                        [pName
                         (sns/encrypt pContent secret-key)
                         (sns/encrypt pNotes secret-key)])
        csv-data (cons [userProfileName userLoginPassword] password-list)]
    (with-out-str
      (csv/write-csv *out* csv-data))))

(defn parse-encrypted-data [data-string]
  (let [entries (str/split data-string #"\n")]
    (into {} (map (fn [entry]
                    (let [[label data] (str/split entry #",")]
                      [label data]))
                  entries))))

(defn read-encrypted-csv [bulk-data]
  (let [csv-content (:csv-content bulk-data) 
        keys (sup/load-keys)
        secret-key (:secret-key keys)
        [user-info & passwords] (str/split csv-content #"\n")
        [existing-username existing-hashed-password] (str/split user-info #",") 
        auth-result (auth/authenticate (:userLoginPassword bulk-data) existing-hashed-password)]
    (if (:authenticated auth-result)
      (let [decrypted-user {:userProfileName existing-username
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