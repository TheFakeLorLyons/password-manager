(ns LPM.clj.io
  (:require [clojure.data.csv :as csv]
            [clojure.pprint :as pp]
            [clojure.string :as str]
            [LPM.clj.setup :as sup]
            [LPM.clj.sensitive :as sns]
            [LPM.clj.auth :as auth]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
                                        ;     converting csv data to atom     ;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn read-edn [data]
  (try
    (let [{:keys [edn-content userProfileName userLoginPassword]} data 
          parsed-info (clojure.edn/read-string edn-content)
          existing-username (:userProfileName parsed-info)
          existing-password (:userLoginPassword parsed-info)
          passwords (:passwords parsed-info)]
      (when (and (= userProfileName existing-username)
                 (auth/authenticate (auth/hash-password userLoginPassword)
                                    existing-password))
        {:userProfileName existing-username
         :userLoginPassword userLoginPassword
         :passwords (mapv (fn [{:keys [id pName pContent pNotes]}]
                            {:id id
                             :pName pName
                             :pContent pContent
                             :pNotes pNotes})
                          passwords)}))
    (catch Exception e 
      (throw (ex-info (str "Error processing EDN:" (ex-message e))
                      {:id ::edn-failed
                       :edn-content data}
                      e)))))

(defn generate-edn [current-user]
  (let [{:keys [userProfileName userLoginPassword passwords]} current-user
        password-list (mapv (fn [{:keys [id pName pContent pNotes]}]
                              {:id id
                               :pName pName
                               :pContent pContent
                               :pNotes pNotes})
                            passwords)
        edn-map {:userProfileName userProfileName
                 :userLoginPassword (auth/hash-password userLoginPassword)
                 :passwords password-list}]
      (with-out-str (pp/pprint edn-map))))

(defn generate-encrypted-edn [current-user]
  (let [keys (sup/load-keys)
        secret-key (:secret-key keys)
        {:keys [userProfileName userLoginPassword passwords]} current-user
        encrypted-passwords (mapv (fn [{:keys [id pName pContent pNotes]}]
                                    {:id id
                                     :pName (sns/encrypt pName secret-key)
                                     :pContent (sns/encrypt pContent secret-key)
                                     :pNotes (sns/encrypt pNotes secret-key)})
                                  passwords)
        edn-map {:userProfileName userProfileName
                 :userLoginPassword (auth/hash-password userLoginPassword)
                 :passwords encrypted-passwords}]
    (with-out-str (pp/pprint edn-map))))

(defn read-encrypted-edn [data]
  (try
    (let [keys (sup/load-keys)
          secret-key (:secret-key keys)
          authenticated-data (read-edn data)]
      (when authenticated-data
        (let [decrypted-passwords (mapv (fn [{:keys [id pName pContent pNotes]}]
                                           {:id id
                                            :pName (sns/decrypt-entry pName secret-key)
                                            :pContent (sns/decrypt-entry pContent secret-key)
                                            :pNotes (sns/decrypt-entry pNotes secret-key)})
                                         (:passwords authenticated-data))]
          {:userProfileName (:userProfileName authenticated-data)
           :userLoginPassword (:userLoginPassword authenticated-data)
           :passwords decrypted-passwords})))
    (catch Exception e
      (println "Error in read-encrypted-csv:" (ex-message e))
      nil)))