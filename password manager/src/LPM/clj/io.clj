(ns LPM.clj.io
  (:require [clojure.data.csv :as csv]
            [clojure.string :as str]
            [LPM.clj.setup :as sup]
            [LPM.clj.sensitive :as sns]
            [LPM.clj.auth :as auth]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
                                        ;     converting csv data to atom     ;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn read-csv [data]
  (try
    (let [{:keys [csv-content userProfileName userLoginPassword]} data
          lines (str/split-lines csv-content)
          [header & password-lines] lines
          [existing-username existing-hashed-password] (str/split header #",")]
      (when (and (= userProfileName existing-username)
                 (auth/authenticate (auth/hash-password userLoginPassword)
                                    existing-hashed-password))
        {:userProfileName existing-username
         :userLoginPassword userLoginPassword
         :passwords (mapv (fn [line]
                            (let [[id pName pContent pNotes] (str/split line #",")]
                              {:id (parse-long id)
                               :pName pName
                               :pContent pContent
                               :pNotes pNotes}))
                          password-lines)}))
    (catch Exception e
      (throw (ex-info (str "Error processing CSV:" (ex-message e))
                      {:id ::csv-failed
                       :csv-string (:csv-content data)}
                      e)))))

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
  (try
    (let [keys (sup/load-keys)
          secret-key (:secret-key keys)
          authenticated-data (read-csv data)]
      (when authenticated-data
        (let [decrypted-passwords (mapv (fn [password-info]
                                           {:id (:id password-info)
                                            :pName (sns/decrypt-entry (:pName password-info) secret-key)
                                            :pContent (sns/decrypt-entry (:pContent password-info) secret-key)
                                            :pNotes (sns/decrypt-entry (:pNotes password-info) secret-key)})
                                         (:passwords authenticated-data))]
          {:userProfileName (:userProfileName authenticated-data)
           :userLoginPassword (:userLoginPassword authenticated-data)
           :passwords decrypted-passwords})))
    (catch Exception e
      (println "Error in read-encrypted-csv:" (ex-message e))
      nil)))