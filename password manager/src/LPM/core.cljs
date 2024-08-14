(ns LPM.core
  (:require [reagent.core :as r]
            [reagent.dom :as rdom]
            [cljs.reader :as reader]
            [reagent.ratom :refer [reaction]]
            [clojure.walk :refer [keywordize-keys]]
            [cljs.core.async :refer [<! timeout go go-loop]]
            [ajax.core :as ajax]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
                                        ;                 Frame               ;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn read-file [file]
  (let [reader (js/FileReader.)]
    (set! (.-onload reader)
          (fn [event]
            (let [csv-data (-> (.-target event) .-result)]
              ;; Process the CSV data here
              (js/console.log csv-data))))
    (.readAsText reader file)))

(defn directory-box []
  (let [file (r/atom nil)
        file-name (r/atom "No file selected")]
    (fn []
      [:div.directory-container
       [:input {:type "file"
                :id "directory-path"
                :name "directory-path"
                :placeholder "Directory Path"
                :accept ".csv"
                :style {:display "none"}
                :on-change (fn [e]
                             (let [selected-file (-> e .-target .-files (aget 0))]
                               (reset! file selected-file)
                               (reset! file-name (.-name selected-file))))}]
       [:div.selected-csv-path-container
        (str "Selected File: " @file-name)]
       [:button {:on-click (fn []
                             (when @file
                               (read-file @file))
                             (-> (js/document.getElementById "directory-path") .click))}
        "Select Directory"]])))

(defn name-and-password-input []
  [:form.login-container 
   [:input {:type "text"
            :id "profileName"
            :name "username"
            :placeholder "Profile Name"
            :required true}]
   [:input {:type "password"
            :id "password"
            :name "password"
            :placeholder "Password"
            :required true}]
   [:input {:type "submit"
            :value "Login"}]])

(defn heading-box []
  [:div.heading-container
   [:h1 "Lor's Password Manager"]])

(defn main-page-input-container []
  [:div.main-container
   [heading-box]
   [:h2 "Login"]
   [directory-box]
   [name-and-password-input]])

(defn login-page [] 
  (main-page-input-container))

(defn ^:dev/after-load start []
  (rdom/render [login-page]
            (.getElementById js/document "app")))

(start)