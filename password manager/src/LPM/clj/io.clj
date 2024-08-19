(ns LPM.clj.io
  (:require [clojure.data.csv :as csv]
            [LPM.clj.user :as usr]
            [clojure.java.io :as io]
            [clojure.string :as str])
  (:import (java.time Instant)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
                                        ;     converting csv data to atom     ;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn csv-to-current-user [csv-string]
  (let [lines (str/split-lines csv-string)
        _ (println "Split lines:" lines "\n")
        [header & password-lines] lines
        _ (println "Header:" header "Password lines:" password-lines "\n")
        [profile-name login-password] (str/split header #",")
        _ (println "Profile name:" profile-name "Login password:" login-password "\n")
        parsed-passwords (mapv (fn [line]
                                 (let [[pName pContent pNotes] (str/split line #",")] ; Split each line into fields
                                   {:pName pName
                                    :pContent pContent
                                    :pNotes pNotes}))
                               password-lines)]
    (println "Parsed passwords:" parsed-passwords "\n")
    {:users
     {profile-name
      {:userProfileName profile-name
       :userLoginPassword login-password
       :passwords parsed-passwords}}}))

(defn current-user-to-csv-data []
  (cons
   ["profile-name" "Profile Name" "Login Password" "Password Name" "Password Content" "Password Notes"]
   (mapcat (fn [[profile-name {:keys [userProfileName userLoginPassword passwords]}]]
             (cons [profile-name userProfileName userLoginPassword]
                   (map (fn [{:keys [pName pContent pNotes]}]
                          [profile-name userProfileName userLoginPassword pName pContent pNotes])
                        passwords)))
           (:users @usr/current-user))))

(defn write-to-csv [current-user]
  (with-open [writer (io/writer "resources/export.csv")]
    (let [users (vals (:users @current-user))]
      (doseq [user users]
        (let [profile-row [[(:userProfileName user) (:userLoginPassword user)]]
              password-rows (map (fn [{:keys [pName pContent pNotes]}]
                                   [pName pContent pNotes])
                                 (:passwords user))]
          ;Write profile row
          (csv/write-csv writer profile-row)
          ;Write password rows
          (csv/write-csv writer password-rows))))))

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
