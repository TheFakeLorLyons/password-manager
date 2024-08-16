(ns LPM.clj.io
  (:require [clojure.data.csv :as csv]
            [LMP.user :as usr]
            [clojure.java.io :as io])
  (:import (java.time Instant)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
                                        ;     converting csv data to atom     ;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn csv-to-current-user [csv-data]
  (reduce (fn [acc [profile-name login-password & password-data]]
            (assoc-in acc [:users profile-name]
                      {:userProfileName profile-name
                       :userLoginPassword login-password
                       :passwords (partition 3 password-data)}))
          {}
          (rest csv-data)))  ; Skip the header row

(defn current-user-to-csv-data []
  (cons
   ["profile-name" "Profile Name" "Login Password" "Password Name" "Password Content" "Password Notes"]
   (mapcat (fn [[profile-name {:keys [userProfileName userLoginPassword passwords]}]]
             (cons [profile-name userProfileName userLoginPassword]
                   (map (fn [{:keys [pName pContent pNotes]}]
                          [profile-name userProfileName userLoginPassword pName pContent pNotes])
                        passwords)))
           (:users @usr/current-user))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
                                        ;          exporting to csv           ;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; Reading from CSV
(defn read-csv []
  (with-open [reader (io/reader "resources/export.csv")]
    (doall
     (csv/read-csv reader))))

(defn write-current-user-to-csv []
  (with-open [writer (io/writer "resources/export.csv")]
    (csv/write-csv writer (current-user-to-csv-data))))
