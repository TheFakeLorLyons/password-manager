(ns LPM.clj.routes
  (:require [compojure.core :refer [defroutes POST GET]]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
            [ring.middleware.session :refer [wrap-session]]
            [ring.middleware.session.cookie :refer [cookie-store]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.cors :refer [wrap-cors]]
            [buddy.auth.middleware :refer [wrap-authentication]]
            [LPM.clj.handlers :as hnd]
            [LPM.clj.auth :as auth]))

(defroutes app-routes
  (POST "/create-account" [] hnd/create-account)
  (POST "/request-existing-csv" [] hnd/request-existing-csv)
  (POST "/save-current-session" [] hnd/save-current-session)
  (POST "/generate-keys" [] hnd/generate-keys-handler)
  (POST "/import-encrypted-csv" [] hnd/import-encrypted)
  (POST "/export-encrypted-csv" [] hnd/export-encrypted-csv)
  (POST "/save-keys" [] hnd/save-keys)

  (GET "/generate-a-password" [] hnd/generate-a-password)
  (GET "/check-setup-status" [] hnd/check-setup-status))

(def handler
  (-> app-routes
      (wrap-params)
      (wrap-cors :access-control-allow-origin  #".*"
                 :access-control-allow-methods [:get :post :delete :options])
      (wrap-session {:store (cookie-store)})
      (wrap-json-body)
      (wrap-json-response)
      (wrap-authentication auth/auth-backend)))

(defn -main [& args]
  (run-jetty handler {:port 3000 :join? false}))

(comment (-main)
         

(defonce server (atom nil)) 
         
         (defn start-server []
           (when @server
             (.stop @server))  ; Stop the existing server if it's running
           (reset! server (run-jetty #'handler {:port 3000 :join? false})))
         
         (defn stop-server []
           (when @server
             (.stop @server)
             (reset! server nil)))  ; Clear the server reference after stopping
         
         (defn restart-server []
           (stop-server)
           (start-server)))
