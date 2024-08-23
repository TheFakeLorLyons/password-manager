(ns LPM.clj.handlers
  (:require [compojure.core :refer [defroutes POST GET DELETE]]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
            [ring.middleware.cors :refer [wrap-cors]]
            [clojure.data.json :as cjson]
            [LPM.clj.pwfuncs :as pwf]
            [LPM.clj.io :as io]))

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
           (do
             (println "CSV credentials successfully processed" - user-data)
             {:status 200
              :headers {"Content-Type" "application/json"}
              :body (cjson/write-str user-data)})
           (do
             (println "Invalid Credentials")
             {:status 401
              :headers {"Content-Type" "application/json"}
              :body (cjson/write-str {:message "Login failed. Profile name or password mismatch."})})))))
(defn save-current-session [request]
  (let [body (:body request)
        profile-name (get body "userProfileName")
        login-password (get body "userLoginPassword")
        passwords (get body "passwords")
        user-profile {:users {"profile" {:userProfileName profile-name
                                         :userLoginPassword login-password
                                         :passwords passwords}}}
        csv-content (io/generate-csv user-profile)]
    (println "Received request body:" body)
    (println "Generated namw:" profile-name)
    (println "Generated pw:" login-password)
    (println "Received pws:" passwords)
    (println "Generated user-profile:" user-profile)
    (println "Generated CSV content:" csv-content)
    (println "Back End attempt to save csv")
    (if csv-content
      (do
        (println "Login successful")
        {:status 200
         :headers {"Content-Type" "text/csv"
                   "Content-Disposition" "attachment; filename=\"passwords.csv\""}
         :body csv-content})
      (do
        (println "Export failed")
        {:status 401
         :headers {"Content-Type" "application/json"}
         :body (cjson/write-str {:message "Exporting user profile failed"})}))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
                                        ;            PW Generation            ;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn generate-a-password [request]
    (println "Received pws:" request)
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