(ns LPM.clj.handlers
  (:require [clojure.data.json :as cjson]
            [LPM.clj.auth :as auth]
            [LPM.clj.pwfuncs :as pwf]
            [LPM.clj.io :as io]
            [LPM.clj.setup :as sup]
            [LPM.clj.user :as usr]
            [clojure.java.io :as jio]
            [cognitect.transit :as transit]
            [ring.util.response :as response])
  (:import [java.io ByteArrayInputStream ByteArrayOutputStream]))

(defn to-transit [data]
  (let [out (ByteArrayOutputStream. 4096)
        writer (transit/writer out :json-verbose)]
    (transit/write writer data)
    (.toString out)))

(defn from-transit [transit-data]
  (let [in (ByteArrayInputStream. (.getBytes (slurp transit-data)))
        reader (transit/reader in :json-verbose)]
    (transit/read reader)))

#_(def in (ByteArrayInputStream. (.toByteArray out)))
#_(def reader (transit/reader in :json))
#_(prn (transit/read reader))  ;; => "foo"
#_(prn (transit/read reader)) 
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
                                        ;                 IO                  ;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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
        csv-content (io/generate-csv body)] 
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
       :headers {"Content-Type" "application/json"};content type for transit
       :body (cjson/write-str decrypted-data)};transit write-transit
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
        #_(response/response file-content)
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

(defn generate-a-password [size]
  (if (and size (pos? size))
    (let [password (pwf/generate-password size)]
      (-> (to-transit {:password password
                       :message "Password generated successfully password"})
          (response/response)
          (response/content-type "application/transit+json")))
    (-> (to-transit {:error "Invalid size parameter. Must be a positive integer."})
        (response/bad-request)
        (response/content-type "application/transit+json"))))

(defn create-account [request]
  (let [data (from-transit (:body request))
        {:keys [userProfileName userLoginPassword]} data]
    (if (and userProfileName userLoginPassword)
      (let [user-profile (usr/create-account userProfileName userLoginPassword)]
        (-> (to-transit {:user-profile user-profile
                         :message "Account generated successfully password"})
            (response/response)
            (response/content-type "application/transit+json")))
      (-> (to-transit {:error "Login failed. Profile name or password mismatch."})
          (response/bad-request)
          (response/content-type "application/transit+json")))))

