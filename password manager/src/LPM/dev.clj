(ns LPM.dev
  (:gen-class)
  (:require [LPM.clj.routes :as routes]
            [shadow.cljs.devtools.api :as shadow]
            [shadow.cljs.devtools.server :as shadow-server]
            [ring.adapter.jetty :refer [run-jetty]]))

(defonce server-ready (atom false))
(defonce server-instance (atom nil))

(defn start [& args]
  (reset! server-ready false)
  (let [server (run-jetty routes/handler {:port 3000 :join? false})]
    (reset! server-ready true)
    server))

(defn start-server! []
  (reset! server-instance (start))
  (reset! server-ready true))

(defn stop-server! []
  (when @server-instance
    (.stop @server-instance)
    (reset! server-instance nil)
    (reset! server-ready false)))

(defn start-frontend! []
  (shadow-server/start!)
  (shadow/watch :app))

(defn stop-frontend! []
  (shadow-server/stop!))

(defn start-dev! []
  (start-server!)
  (start-frontend!))

(defn stop-dev! []
  (stop-server!)
  (stop-frontend!))

(defn restart-dev! []
  (stop-dev!)
  (start-dev!))

(defn -main [& args]
  (start-dev!))