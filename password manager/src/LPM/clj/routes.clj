(ns LPM.clj.routes
  (:require [compojure.core :refer [defroutes POST GET DELETE]]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
            [ring.middleware.session :refer [wrap-session]]
            [ring.middleware.session.cookie :refer [cookie-store]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.cors :refer [wrap-cors]]
            [LPM.clj.handlers :as hnd]))

(defroutes app-routes
  (POST "/request-existing-csv" [] hnd/request-existing-csv)

  (POST "/save-current-session" [] hnd/save-current-session)

  (GET "/generate-a-password" [] hnd/generate-a-password))

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