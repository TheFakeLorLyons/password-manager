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
                          (let [[id pName pContent pNotes] (str/split line #",")]
                            {:id (parse-long id)
                             :pName pName
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
        password-list (for [{:keys [id pName pContent pNotes]} passwords]
               [id pName pContent pNotes]) 
        csv-data (cons [userProfileName userLoginPassword] password-list)]
    (with-out-str
      (csv/write-csv *out* csv-data))))

(defn generate-encrypted-csv [current-user]
  (let [keys (sup/load-keys) 
        secret-key (:secret-key keys) 
        {:keys [id userProfileName userLoginPassword passwords]} current-user
        password-list (for [{:keys [id pName pContent pNotes]} passwords]
                        [id
                         (sns/encrypt pName secret-key)
                         (sns/encrypt pContent secret-key)
                         (sns/encrypt pNotes secret-key)])
        csv-data (cons [userProfileName userLoginPassword] password-list)]
    (with-out-str
      (csv/write-csv *out* csv-data))))

(defn read-encrypted-csv [data]
  (let [csv-content (:csv-content data) 
        keys (sup/load-keys)
        secret-key (:secret-key keys)
        [user-info & passwords] (str/split csv-content #"\n")
        [existing-username existing-hashed-password] (str/split user-info #",") 
        auth-result (auth/authenticate (:userLoginPassword data) existing-hashed-password)]
    (if (:authenticated auth-result)
      (let [decrypted-user {:userProfileName existing-username
                            :userLoginPassword existing-hashed-password}
            decrypted-passwords (for [password-line passwords
                                      :let [[id encrypted-name encrypted-content encrypted-notes] (str/split password-line #",")]]
                                  {:id (parse-long id)
                                   :pName (sns/decrypt-entry encrypted-name secret-key)
                                   :pContent (sns/decrypt-entry encrypted-content secret-key)
                                   :pNotes (sns/decrypt-entry encrypted-notes secret-key)})]
        {:authenticated true
         :userProfileName (:userProfileName decrypted-user)
         :userLoginPassword (:userLoginPassword decrypted-user)
         :passwords decrypted-passwords})
      {:authenticated false})))