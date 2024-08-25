(ns LPM.clj.handlers
   (:require [clojure.data.json :as cjson]
             [clojure.data.csv :as csv]
             [LPM.clj.pwfuncs :as pwf]
             [LPM.clj.io :as io]
             [LPM.clj.setup :as sup]
             [LPM.clj.sensitive :as sns]
             [clojure.edn :as edn]
             [clojure.java.io :as jio]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
                                        ;                 IO                  ;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

 (defn request-existing-csv [request]
   (let [body (:body request)
         profile-name (get body "userProfileName")
         login-password (get body "userLoginPassword")
         user-data  (io/csv-to-current-user (get body "csv-content"))]
     (and user-data
          (if (and (= profile-name (:userProfileName user-data))
                   (= login-password (:userLoginPassword user-data)))
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

 (defn export-encrypted [request]
   (let [body (:body request)
         user-profile {:users {"profile" body}}
         csv-content (io/generate-encrypted-csv user-profile)]
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
   (let [body (:body request)
         keys (get body "arr")
         secret-key (nth keys 1)
         public-key (nth keys 3)
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
 
 (defn mark-setup-complete [request]
   (println "save-keys-handler called" request)
   (let [body (:body request)
         keys (get body "arr")
         secret-key (nth keys 1)
         public-key (nth keys 3)
         saved-keys {:secret-key secret-key
                       :public-key public-key}
         marked-file (sup/mark-setup-complete saved-keys)]
     (println "received keys [hopefully] " saved-keys)
     (if marked-file
       {:status 200
        :headers {"Content-Type" "application/json"}
        :body (cjson/write-str saved-keys)}
       {:status 500
        :headers {"Content-Type" "application/json"}
        :body (cjson/write-str {:message "Failed to mark setup file... "})})))
 
 (defn check-setup-status [request]
   (println "checking setup status...")
   (if (.exists (jio/file sup/key-file))
     (let [file-content (slurp sup/key-file)
           _ (println "File content:" file-content)]
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