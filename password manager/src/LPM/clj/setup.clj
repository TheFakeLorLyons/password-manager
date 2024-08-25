(ns LPM.clj.setup
  (:require [clojure.java.io :as io]
            [clojure.edn :as edn])
  (:import [java.security SecureRandom]
           [java.util Base64]))

(def key-file "keys.edn")

(defn- bytes->hex [bytes]
  (.encodeToString (Base64/getEncoder) bytes))

(defn generate-keys []
  (let [key-gen (SecureRandom.)
        secret-key (byte-array 32)
        public-key (byte-array 32)]
    (.nextBytes key-gen secret-key)
    (.nextBytes key-gen public-key)
    (let [secret-key-hex (bytes->hex secret-key)
          public-key-hex (bytes->hex public-key)]
      {:secret-key secret-key-hex
       :public-key public-key-hex})))

(defn- bytes->hex [bytes]
  (.encodeToString (Base64/getEncoder) bytes))

(defn save-keys [keys]
  (println "saving keys in setup.clj")
  (spit key-file (pr-str keys)))

(defn load-keys []
  (println "loading keys in setup.clj")
  (if (.exists (io/file key-file))
    (read-string (slurp key-file))
    nil))

(defn mark-setup-complete [keys]
  (println "marking-setup-complete in setup.clj")
  (let [setup-data {:setup-complete true
                    :completed-at (java.time.Instant/now)
                    :public-key (:public-key keys)}]
    (spit key-file (pr-str setup-data))))