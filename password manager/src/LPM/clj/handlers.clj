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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
                                        ;                 IO                  ;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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

(defn extract-user-data [data]
  (let [{:keys [csv-content userProfileName userLoginPassword]} data
        user-data  (io/csv-to-current-user csv-content)
        pw-to-compare (:userLoginPassword user-data)
        username-to-compare (:userProfileName user-data)]
    (when (and (= userProfileName username-to-compare)
               (auth/authenticate (auth/hash-password userLoginPassword) pw-to-compare))
      user-data)))

(defn import-csv [request]
  (let [data (from-transit (:body request)) 
        user-data (extract-user-data data)]
    (if user-data
      (-> (to-transit {:user-data user-data
                       :message "Successfully imported CSV"})
          (response/response)
          (response/content-type "application/transit+json"))
      (-> (to-transit {:error "Importing the CSV failed, error: "})
          (response/bad-request)
          (response/content-type "application/transit+json")))))

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
       :headers {"Content-Type" "application/json"}
       :body (cjson/write-str decrypted-data)}
      {:status 401
       :headers {"Content-Type" "application/json"}
       :body (cjson/write-str {:message "Importing encrypted profile failed"})})))

(defn generate-keys [request]
  (try
    (-> (to-transit (sup/generate-keys))
        (response/response)
        (response/content-type "application/transit+json"))
    (catch Exception event
      (-> (to-transit {:error (str "Exception during key generation: " event)})
          (response/bad-request)
          (response/content-type "application/transit+json")))))

(defn save-keys [request]
  (let [body (:body request)    ; Convert response body from JSON
        arr (get body "arr")    ; Extract the array from the body
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
  (try
    (-> (to-transit (slurp sup/key-file))
        (response/response)
        (response/content-type "application/transit+json"))
    (catch Exception event
      (-> (to-transit {:error (str "Error, No key file located: " event)})
          (response/bad-request)
          (response/content-type "application/transit+json")))))

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
