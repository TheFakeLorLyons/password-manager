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
  (try
    (let [lines (str/split-lines csv-string)
          [header & password-lines] lines
          [profile-name login-password] (str/split header #",")]
      {:userProfileName profile-name
       :userLoginPassword login-password
       :passwords (mapv (fn [line]
                          (let [[pName pContent pNotes] (str/split line #",")]
                            {:pName pName
                             :pContent pContent
                             :pNotes pNotes}))
                        password-lines)})
    (catch Exception e
      (println "Error processing CSV:" (.getMessage e))
      nil)))


(defn current-user-to-csv-data []
  (cons
   ["profile-name" "Profile Name" "Login Password" "Password Name" "Password Content" "Password Notes"]
   (mapcat (fn [[profile-name {:keys [userProfileName userLoginPassword passwords]}]]
             (cons [profile-name userProfileName userLoginPassword]
                   (map (fn [{:keys [pName pContent pNotes]}]
                          [profile-name userProfileName userLoginPassword pName pContent pNotes])
                        passwords)))
           (:users @usr/current-user))))

(defn write-to-csv [current-user path]
  (with-open [writer (io/writer path)]
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

(defn generate-csv [current-user]
  (let [profile (get-in current-user [:users "profile"])
        user-info [(get profile :userProfileName) (get profile :userLoginPassword)]
        passwords (get profile :passwords)
        data (for [password passwords]
               [(get password "pName")
                (get password "pContent")
                (get password "pNotes")])
        csv-data (cons user-info data)]
    (with-out-str
      (csv/write-csv *out* csv-data))))

(defn generate-csv2 [current-user]
  (let [output (java.io.StringWriter.)]
    (with-open [writer (io/writer output)]
      (let [users (vals (:users @current-user))]
        (doseq [user users]
          (let [profile-row [[(:userProfileName user) (:userLoginPassword user)]]
                password-rows (map (fn [{:keys [pName pContent pNotes]}]
                                     [pName pContent pNotes])
                                   (:passwords user))]
            (csv/write-csv writer profile-row)
            (csv/write-csv writer password-rows)))))
    (.toString output)))
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
