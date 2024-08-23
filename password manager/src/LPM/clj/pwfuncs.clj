(ns LPM.clj.pwfuncs
  (:require [malli.core :as m]
            [clojure.string :as str])
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
  (println "pwfunc req:" length)
  (let [chars "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()_+-=[]{}|;:,.<>?/"]
    (apply str
           (repeatedly length
                       #(rand-nth chars)))))