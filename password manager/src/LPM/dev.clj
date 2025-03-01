(ns LPM.dev
  (:gen-class)
  (:require [LPM.clj.routes :as routes]
            [shadow.cljs.devtools.api :as shadow]
            [shadow.cljs.devtools.server :as shadow-server]
            [ring.adapter.jetty :refer [run-jetty]]
            [clojure.string :as str]))

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

(comment
  (start-dev!)
  (stop-dev!)
  (restart-dev!)

  (defn circumference [r]
    (apply * [2 Math/PI r]))

  (defn area [r]
    (* (Math/pow r 2) Math/PI))

  (def coord
    {:x 0
     :y 0})

  (def origin
    coord)

  (def polar
    {:x 0
     :deg 75})

  (def socrates)

  (def page
    {:is "musician"
     :was ""})

  (def destination
    (let [x 15]
      {:x x
       :deg (:deg polar)}))

  (defn slope [p1 p2]
    (let [x1 (:x p1)
          x2 (:x p2)
          y1 (:y p1)
          y2 (:y p2)]
      (/ (- y2 y1) (- x2 x1))))

  (defn get-slope [x y]
    (let [origin coord
          p2 {:x x :y y}]
      (slope origin p2)))
  (slope coord {:x 3 :y 5})
  (float (get-slope 3 5))

  (:deg destination)
  (area 7)
  (circumference 5)

  (defn clj-sentence [ending]
    (str "It's nice to be able to evaluate things " ending))

  (clj-sentence "at the repl")

  (defn fractal-carrots [height]
    (loop [carrot "^"
           donkey-traversal 1
           destination height
           pre-spaces (/ height 2)]
      (when (<= donkey-traversal destination)
        (let [spaces (apply str (repeat  (- pre-spaces destination) " "))] 
          (println (str spaces (apply str (repeat donkey-traversal carrot)))))
        (recur carrot (inc donkey-traversal)  destination (- 2 pre-spaces)))))
  
  (fractal-carrots 256)
  )