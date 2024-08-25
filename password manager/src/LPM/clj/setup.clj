(ns LPM.clj.setup
  (:require [clojure.java.io :as io]
            [clojure.data.json :as cjson]
            [clojure.edn :as edn])
  (:import [java.security SecureRandom]
           [java.util Base64]))

(def key-file "keys.edn")

(defn- bytes->hex [bytes]
  (.encodeToString (Base64/getEncoder) bytes))

(defn base64->bytes [base64-str]
  (.decode (Base64/getDecoder) base64-str))

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
  (let [current-time (java.time.Instant/now)
        keys-with-metadata {:setup-complete true
                            :completed-at current-time
                            :public-key (:public-key keys)
                            :secret-key (:secret-key keys)}]
    (spit key-file (cjson/write-str keys-with-metadata))
    (println "saved?" keys-with-metadata)
    keys-with-metadata))

(defn load-keys []
  (let [file-content (slurp key-file)
        _ (println "loaded parsed content" file-content)
        parsed-content (cjson/read-str file-content :key-fn keyword)]
    (println "loaded parsed content" parsed-content)
    {:public-key (base64->bytes (:public-key parsed-content))
     :secret-key (base64->bytes (:secret-key parsed-content))}))