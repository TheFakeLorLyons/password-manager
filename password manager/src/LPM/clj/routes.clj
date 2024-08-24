(ns LPM.clj.routes
  (:require [compojure.core :refer [defroutes POST GET]]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
            [ring.middleware.session :refer [wrap-session]]
            [ring.middleware.session.cookie :refer [cookie-store]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.cors :refer [wrap-cors]]
            [LPM.clj.handlers :as hnd]
            [LPM.clj.setup :as sup]))

(defroutes app-routes
  (POST "/request-existing-csv" [] hnd/request-existing-csv)
  (POST "/save-current-session" [] hnd/save-current-session)
  (POST "/export-encrypted" [] hnd/export-encrypted)
  (POST "/mark-setup-complete" [] sup/mark-setup-complete)
  (POST "/generate-keys" [] hnd/generate-keys-handler)
  (POST "/import-encrypted" [] hnd/import-encrypted)
  (POST "/export-encrypted" [] hnd/export-encrypted)

  (GET "/generate-a-password" [] hnd/generate-a-password)
  (GET "/check-setup-status" [] sup/check-setup-status))

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