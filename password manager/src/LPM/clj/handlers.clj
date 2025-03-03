(ns LPM.clj.handlers
  (:require [LPM.clj.pwfuncs :as pwf]
            [LPM.clj.io :as io]
            [LPM.clj.setup :as sup]
            [LPM.clj.user :as usr]
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
                         :message "Account generated successfully"})
            (response/response)
            (response/content-type "application/transit+json")))
      (-> (to-transit {:error "Login failed. Profile name or password mismatch."})
          (response/bad-request)
          (response/content-type "application/transit+json")))))

(defn export-csv [request]
  (let [body (from-transit (:body request)) 
        csv-content (io/generate-csv body)]
    (try
      (-> (to-transit {:user-data csv-content
                       :message "Successfully exported CSV"})
          (response/response)
          (response/content-type "text/csv")
          (response/header "Content-Disposition" "attachment; filename=\"passwords.csv\""))
      (catch Exception error
        (-> (to-transit {:error (str "Exporting the CSV failed, error: " error)})
            (response/bad-request)
            (response/content-type "text/csv"))))))

(defn import-csv [request]
  (let [csv-data (from-transit (:body request))]
  (try
    (-> (to-transit {:user-data (io/read-csv csv-data)
                     :message "Successfully imported CSV"})
        (response/response)
        (response/content-type "application/transit+json"))
    (catch Exception error
      (-> (to-transit {:error (str "Importing the CSV failed, error: " error)})
          (response/bad-request)
          (response/content-type "application/transit+json"))))))

(defn export-encrypted-csv [request]
  (let [body (from-transit (:body request))
        csv-data (io/generate-encrypted-csv body)] 
      (try
        (-> (to-transit {:user-data csv-data
                         :message "Successfully exported CSV"})
            (response/response)
            (response/content-type "text/csv")
            (response/header "Content-Disposition" "attachment; filename=\"encrypted.csv\""))
        (catch Exception error
          (-> (to-transit {:error (str "Exporting the CSV failed, error: " error)})
              (response/bad-request)
              (response/content-type "text/csv"))))))

(defn import-encrypted [request]
  (let [csv-data (from-transit (:body request)) 
        decrypted-data (io/read-encrypted-csv csv-data)]
    (try
      (-> (to-transit {:user-data decrypted-data
                       :message "Successfully imported CSV"})
          (response/response)
          (response/content-type "application/transit+json"))
      (catch Exception error
        (-> (to-transit {:error (str "Importing the CSV failed, error: " error)})
            (response/bad-request)
            (response/content-type "application/transit+json"))))))


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
  (let [body (from-transit (:body request)) 
        {:keys [public-key secret-key]} (:keys body)
        saved-keys (sup/save-keys public-key secret-key)]
    (if saved-keys
      (-> (to-transit {:keys ({:keys [public-key secret-key]} saved-keys)
                       :message "Keys saved successfully"})
          (response/response)
          (response/content-type "application/transit+json"))
      (-> (to-transit {:error "Keys failed to save."})
          (response/bad-request)
          (response/content-type "application/transit+json")))))

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

(defn generate-a-password [request]
  (let [body (from-transit (:body request))
        size (parse-long (:size body))]
    (if (and size (pos? size))
      (-> (to-transit {:password (pwf/generate-password size)
                       :message "Password generated successfully password"})
          (response/response)
          (response/content-type "application/transit+json"))
      (-> (to-transit {:error "Invalid size parameter. Must be a positive integer."})
          (response/bad-request)
          (response/content-type "application/transit+json")))))
