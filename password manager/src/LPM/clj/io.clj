(ns LPM.clj.io
  (:require [clojure.data.csv :as csv]
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
