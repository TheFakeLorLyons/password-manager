(ns LPM.clj.handlers
  (:require [clojure.data.json :as cjson]
            [clojure.data.csv :as csv]
            [LPM.clj.auth :as auth]
            [LPM.clj.pwfuncs :as pwf]
            [LPM.clj.io :as io]
            [LPM.clj.setup :as sup]
            [LPM.clj.user :as usr]
            [LPM.clj.sensitive :as sns]
            [clojure.edn :as edn]
            [clojure.java.io :as jio]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
                                        ;                 IO                  ;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn create-account [request]
  (let [body (:body request)
        profile-name (get body "userProfileName")
        login-password (get body "userLoginPassword")
        user-profile (usr/create-account profile-name login-password)]
    (if (and profile-name login-password)
      {:status 200
       :headers {"Content-Type" "application/json"}
       :body user-profile}
      {:status 401
       :headers {"Content-Type" "application/json"}
       :body (cjson/write-str {:message "Login failed. Profile name or password mismatch."})})))

(defn request-existing-csv [request]
  (let [body (:body request)
        keys (sup/generate-keys)
        profile-name (get body "userProfileName")
        login-password (get body "userLoginPassword")
        user-data  (io/csv-to-current-user (get body "csv-content"))
        pw-to-compare (:userLoginPassword user-data)]
    (and user-data
         (if (and (= profile-name (:userProfileName user-data))
                  (auth/authenticate (auth/hash-password login-password) pw-to-compare))
           {:status 200
            :headers {"Content-Type" "application/json"}
            :body (cjson/write-str user-data)}
           {:status 401
            :headers {"Content-Type" "application/json"}
            :body (cjson/write-str {:message "Login failed. Profile name or password mismatch."})}))))

(defn save-current-session [request]
  (let [body (:body request)
        profile-name (get body "userProfileName")
        login-password (get body "userLoginPassword")
        passwords (get body "passwords")
        user-profile {:users {"profile" {:userProfileName profile-name
                                         :userLoginPassword login-password
                                         :passwords passwords}}}
        csv-content (io/generate-csv user-profile)]
    (if csv-content
      {:status 200
       :headers {"Content-Type" "text/csv"
                 "Content-Disposition" "attachment; filename=\"passwords.csv\""}
       :body csv-content}
      {:status 401
       :headers {"Content-Type" "application/json"}
       :body (cjson/write-str {:message "Exporting user profile failed"})})))

(defn export-encrypted-csv [request] 
  (let [body (:body request) 
        csv-content (io/generate-encrypted-csv body)]
    (if csv-content
      {:status 200
       :headers {"Content-Type" "text/csv"
                 "Content-Disposition" "attachment; filename=\"encrypted.csv\""}
       :body csv-content}
      {:status 401
       :headers {"Content-Type" "application/json"}
       :body (cjson/write-str {:message "Exporting encrypted profile failed"})})))

(defn import-encrypted [request]
  (let [csv-data (:body request) 
        decrypted-data (io/read-encrypted-csv csv-data)]
    (if decrypted-data
      {:status 200
       :headers {"Content-Type" "application/json"}
       :body (cjson/write-str decrypted-data)}
      {:status 401
       :headers {"Content-Type" "application/json"}
       :body (cjson/write-str {:message "Importing encrypted profile failed"})})))

(defn generate-keys-handler [request]
  (try
    (let [keys (sup/generate-keys)]
      {:status 200
       :headers {"Content-Type" "application/json"}
       :body (cjson/write-str keys)})
    (catch Exception event
      (println "Exception during key generation:" (.getMessage event))
      {:status 500
       :headers {"Content-Type" "application/json"}
       :body (cjson/write-str {:message "Failed to generate keys... "})})))

(defn save-keys [request]
  (let [body (:body request) ; Convert response body from JSON
        arr (get body "arr")  ; Extract the array from the body
        secret-key (get arr 1)  ; First element is secret-key
        public-key (get arr 3)  ; Second element is public-key
        keys-to-save {:secret-key secret-key
                      :public-key public-key}
        saved-keys (sup/save-keys keys-to-save)]
    (if saved-keys
      {:status 200
       :headers {"Content-Type" "application/json"}
       :body (cjson/write-str saved-keys)}
      {:status 500
       :headers {"Content-Type" "application/json"}
       :body (cjson/write-str {:message "Failed to save keys... "})})))

(defn check-setup-status [request]
  (if (.exists (jio/file sup/key-file))
    (let [file-content (slurp sup/key-file)]
      (if file-content
        {:status 200
         :headers {"Content-Type" "application/json"}
         :body (cjson/write-str file-content)}
        {:status 401
         :headers {"Content-Type" "application/json"}
         :body (cjson/write-str {:message "Failed to read setup file... "})}))
    {:status 500
     :headers {"Content-Type" "application/json"}
     :body (cjson/write-str {:message "Keys have not been generated yet"})}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
                                        ;            PW Generation            ;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn generate-a-password [request]
  (let [size (try
               (Integer/parseInt (get-in request [:params "size"]))
               (catch Exception e
                 nil))]
    (if (and size (pos? size))
      (let [password (pwf/generate-password size)]
        {:status 200
         :body {:password password
                :message "Password generated successfully password"}})
      {:status 400
       :body {:error "Invalid size parameter. Must be a positive integer."}})))
