(ns LPM.clj.routes
  (:require [compojure.core :refer [defroutes POST GET DELETE]]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
            [ring.middleware.session :refer [wrap-session]]
            [ring.middleware.session.cookie :refer [cookie-store]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.file :refer (wrap-file)]
            [ring.middleware.cors :refer [wrap-cors]]
            [clojure.data.json :as cjson]
            [LPM.clj.user :as usr]
            [LPM.clj.pwfuncs :as pwf]
            [LPM.clj.io :as io]))

(defroutes app-routes
  (POST "/create-account" {:keys [body]}
    (let [profile-name (:userProfileName body)
          login-password (:userLoginPassword body)]
      (usr/on-create-account profile-name login-password)
      (println "Received request body:" body)
      (println "Generated user-profile:" login-password)
      (println "Generated CSV content:")
      (println "Back End attempt to save csv")
      {:status 200
       :headers {"Content-Type" "application/json"}
       :body (cjson/write-str {:message "User created successfully"})}))

  (POST "/add-a-new-password" {:keys [body]}
    (let [profile-name (:userProfileName body)
          login-password (:userLoginPassword body)
          passwords (:passwords body)]
      (usr/add-new-password profile-name login-password passwords)
      {:status 200
       :headers {"Content-Type" "application/json"}
       :body (cjson/write-str {:message "L-> Password added successfully"})}))

  (DELETE "/remove-a-password" {:keys [body]}
    (let [profile-name (:userProfileName body)
          pName (:pName body)]
      (usr/remove-a-password profile-name pName)
      {:status 200
       :headers {"Content-Type" "application/json"}
       :body (cjson/write-str {:message "L-> Password removed successfully"})}))

  (POST "/request-existing-csv" {:keys [body]} 
    (println "Back End attempt to read csv" body)
    (let [profile-name (get body "userProfileName")
          login-password (get body "userLoginPassword")
          user-data (io/csv-to-current-user (get body "csv-content"))]
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

  (POST "/save-current-session" {:keys [body]}
    (let [profile-name (get body "userProfileName")
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

  (GET "/generate-a-password" request
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
         :body {:error "Invalid size parameter. Must be a positive integer."}}))))

(def handler
  (-> app-routes
      (wrap-params)
      (wrap-cors :access-control-allow-origin  #".*"
                 :access-control-allow-methods [:get :post :delete :options])
      (wrap-session {:store (cookie-store)})
      (wrap-json-body)
      (wrap-json-response)))

(defn -main [& args]
  (run-jetty handler {:port 3000 :join? false}))