(ns LPM.clj.pwfuncs
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as sg]
            [malli.core :as m]
            [malli.generator :as mg]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [LPM.clj.user :as usr])
  (:import (java.time Instant))
  (:import [java.security SecureRandom]
           [java.util Base64]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
                                        ; Password Generation w/ Malli and re ;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn percentage [percent total]
  (int (Math/round (double (* (/ percent 100) total)))))

(defn construct-regex [num-char per-u per-sym]
  (let [min-length 8
        max-length num-char
        min-symbols (max 0 (percentage per-sym max-length))
        max-symbols (min max-length (inc min-symbols))
        min-upper (max 0 per-u)
        re-pattern (format "^(?=.*\\d)(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%%^&*()]).{%d,%d}$" min-length max-length)]
    {:pattern re-pattern
     :min-symbols min-symbols
     :max-symbols max-symbols
     :min-upper min-upper}))

(defn dynamic-word-schema [num-char num-space per-u per-sym]
  (let [{:keys [pattern min-length max-length min-symbols max-symbols min-upper]} (construct-regex num-char per-u per-sym)]
    [:and
     [:re pattern]
     [:fn (fn [s]
            (let [char-count (count s)
                  space-count (count (re-seq #"\s" s))
                  upper-case-count (count (re-seq #"[A-Z]" s))
                  special-char-count (count (re-seq #"[!@#$%^&*()]" s))]
              (and (>= char-count min-length)
                   (<= char-count max-length) `(>= upper-case-count min-upper)
                   (and (>= special-char-count min-symbols)
                        (<= special-char-count max-symbols))
                   (<= space-count num-space))))]]))

(defn generate-custom-examples [schema n]
  (let [generate-example (fn []
                           (let [s (apply str (map char (repeatedly (rand-int 10) #(rand-nth (map char (range 32 127))))))]
                             (if (m/validate schema s)
                               s
                               (recur))))]
    (repeatedly n generate-example)))

(defn generate-formatted-examples [schema n]
  (let [examples (generate-custom-examples schema n)]
    (str/join "\n" examples)))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
                                        ; Password Generation w/ spcec        ;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn generate-password [length]
  (let [chars "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()_+-=[]{}|;:,.<>?/"]
    (apply str
           (repeatedly length
                       #(rand-nth chars)))))
(defn gen 
  [size]
  (generate-password size))
  
(defn add-password [current-user profile-name]
  (println "Would you like to create your own password or generate one? (Enter 'create' or 'generate'):")
  (let [choice (str/lower-case (read-line))]
    (case choice
      "create" (do
                 (println "Enter password name:")
                 (let [pName (read-line)]
                   (println "Enter password content:")
                   (let [pContent (read-line)]
                     (println "Enter notes (optional):")
                     (let [pNotes (read-line)]
                       (swap! current-user update-in [:users profile-name :passwords] conj {:pName pName :pContent pContent :pNotes pNotes})
                       (println "Password added successfully!")))))
      "generate" (do
                   (println "Enter password name:")
                   (let [pName (read-line)]
                     (println "Enter desired password length:")
                     (let [length (Integer/parseInt (read-line))
                           pContent (generate-password length)]
                       (println "Generated password:" pContent)
                       (println "Enter notes (optional):")
                       (let [pNotes (read-line)]
                         (swap! current-user update-in [:users profile-name :passwords] conj {:pName pName :pContent pContent :pNotes pNotes})
                         (println "Password added successfully!")))))
      (println "Invalid choice. Please enter 'create' or 'generate'."))))

(defn authenticate [current-user profile-name login-password]
  (let [user (some #(and (= (:uname %) profile-name) (= (:loginPassword %) login-password)) (:users @usr/current-user))]
    (if user
      (do
        (println "Authentication successful.")
        true)
      (do
        (println "Authentication failed.")
        true))))