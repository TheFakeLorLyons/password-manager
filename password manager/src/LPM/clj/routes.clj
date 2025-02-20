(ns LPM.clj.routes
  (:require [compojure.core :refer [defroutes POST GET routes]] 
            [ring.middleware.session :refer [wrap-session]]
            [ring.middleware.session.cookie :refer [cookie-store]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.cors :refer [wrap-cors]]
            [buddy.auth.middleware :refer [wrap-authentication]]
            [LPM.clj.handlers :as h]
            [LPM.clj.auth :as auth]))

(defroutes app-routes
  (POST "/create-account" [] h/create-account)
  (POST "/export-csv" [] h/export-csv)
  (POST "/export-encrypted-csv" [] h/export-encrypted-csv)
  (POST "/generate-a-password" [] h/generate-a-password)
  (POST "/generate-keys" [] h/generate-keys)
  (POST "/import-csv" [] h/import-csv) 
  (POST "/import-encrypted-csv" [] h/import-encrypted)
  (POST "/save-keys" [] h/save-keys)

  (GET "/check-setup-status" [] h/check-setup-status))

(def handler
  (-> app-routes
      (wrap-params)
      (wrap-cors :access-control-allow-origin  #".*"
                 :access-control-allow-methods [:get :post])
      (wrap-session {:store (cookie-store)})
      (wrap-authentication auth/auth-backend)))
