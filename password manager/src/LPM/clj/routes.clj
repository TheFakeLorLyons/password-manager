(ns LPM.clj.routes
  (:require [compojure.core :refer [defroutes POST GET routes]]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
            [ring.middleware.session :refer [wrap-session]]
            [ring.middleware.session.cookie :refer [cookie-store]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.cors :refer [wrap-cors]]
            [buddy.auth.middleware :refer [wrap-authentication]]
            [LPM.clj.handlers :as h]
            [LPM.clj.auth :as auth]))

;coersions with compojure (turn size to int so I don't have to parse)
(defroutes app-routes
  (POST "/create-account" [] h/create-account)
  (POST "/generate-keys" [] h/generate-keys)
  (POST "/import-csv" [] h/import-csv)

  (GET "/check-setup-status" [] h/check-setup-status)
  (GET "/generate-a-password" [size] (h/generate-a-password (parse-long size))))

(defroutes json-endpoints 
  (POST "/save-current-session" [] h/save-current-session)
  (POST "/import-encrypted-csv" [] h/import-encrypted)
  (POST "/export-encrypted-csv" [] h/export-encrypted-csv)
  (POST "/save-keys" [] h/save-keys))

(def json-wrapped-endpoints
  (-> json-endpoints
      (wrap-json-body)
      (wrap-json-response)))

(def handler
  (-> (routes app-routes json-wrapped-endpoints)
      (wrap-params)
      (wrap-cors :access-control-allow-origin  #".*"
                 :access-control-allow-methods [:get :post :delete :options])
      (wrap-session {:store (cookie-store)})
      #_(wrap-json-body)
      #_(wrap-json-response)
      (wrap-authentication auth/auth-backend)))

(defn -main [& args]
  (run-jetty handler {:port 3000 :join? false}))

(comment (-main)
         (defonce server (atom nil))

         (defn start-server []
           (when @server
             (.stop @server))  ;Stop the existing server if it's running
           (reset! server (run-jetty #'handler {:port 3000 :join? false})))

         (defn stop-server []
           (when @server
             (.stop @server)
             (reset! server nil)))  ;Clear the server reference after stopping

         (defn restart-server []
           (stop-server)
           (start-server)))
