(ns LPM.cljs.helpers
  (:require [ajax.core :as ajax]
            [reagent.core :as r]
            [clojure.string :as str]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
                                        ;              csv functions          ;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def csv-content (r/atom nil))

(defn read-file [file callback]
  (let [reader (js/FileReader.)]
    (set! (.-onload reader)
          (fn [event]
            (let [content (.. event -target -result)]
              (callback content))))
    (.readAsText reader file)))

(defn handle-file-selection [file]
  (read-file file
             (fn [content]
               (reset! csv-content content))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
                                        ;                 State               ;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def user-state (r/atom {:userProfileName nil
                         :userLoginPassword nil
                         :passwords []}))

(def logged-in (r/atom false))

(def show-add-form (r/atom false));true to speed up to generation

(def editing-password (r/atom nil))

(defn logout []
  (reset! user-state {:userProfileName nil
                      :userLoginPassword nil
                      :passwords []})
  (reset! logged-in false))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
                                        ;               API Calls             ;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn generate-password-request [size]
  (let [entered-size (if (seq size) size "12")
        url (str "http://localhost:3000/generate-a-password?size=" entered-size)] 
    (js/Promise.
     (fn [resolve reject]
       (ajax/GET url
         {:response-format (ajax/json-response-format {:keywords? true})
          :handler (fn [response]
                     (resolve (get response :password)))
          :error-handler (fn [error]
                           (reject error))})))))

(defn request-existing-csv [profile-name login-password]
  (ajax/POST "http://localhost:3000/request-existing-csv"
    {:params {:csv-content @csv-content
              :userProfileName profile-name
              :userLoginPassword login-password}
     :headers {"Content-Type" "application/json"}
     :format :json
     :response-format :json
     :handler (fn [response]
                (let [profile-name (get response "userProfileName")
                      login-password (get response "userLoginPassword")
                      processed-passwords
                      (doall
                       (mapv
                        (fn [pw]
                          (let [pName (get pw "pName")
                                pContent (get pw "pContent")
                                pNotes (get pw "pNotes")]
                            {:pName pName
                             :pContent pContent
                             :pNotes pNotes}))
                        (get response "passwords")))]
                  (reset! user-state {:userProfileName profile-name
                                      :userLoginPassword login-password
                                      :passwords processed-passwords}))
                (reset! logged-in true))
     :error-handler (fn [error]
                      (reset! logged-in false)
                      (js/console.error "Failed obtain user profile:" error))}))

(defn save-current-session [callback]
  (let [user-profile-name (get @user-state :userProfileName)
        user-login-password (get @user-state :userLoginPassword)
        passwords  (get @user-state :passwords)]
    (ajax/POST "http://localhost:3000/save-current-session"
      {:params {:userProfileName user-profile-name
                :userLoginPassword user-login-password
                :passwords  passwords}
       :headers {"Content-Type" "text/csv"}
       :format :json
       :handler (fn [response]
                  (callback response))
       :error-handler (fn [error]
                        (js/console.error "Failed to export csv:" error))})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
                                        ;                 CRUD                ;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn handle-login-submission
  "This function either creates a blank slate user, or draws exising user
   information from CSV using the above 'request-existing-csv' fn."
  [event profile-name login-password login error-message]
  (.preventDefault event)
  (if (and profile-name login-password (seq @profile-name) (seq @login-password))
    (.preventDefault event)
    (reset! error-message "All fields must be filled in"))
  (if (and (seq @profile-name) (seq @login-password))
    (do
      (reset! error-message "")

      (if @login
        (do
          (js/setTimeout  ;Ensure csv-content is set before making request
           (fn []
             (request-existing-csv @profile-name @login-password)) ;Make API request
           100)
          (reset! logged-in true))
        (do

          (reset! user-state  {:userProfileName @profile-name
                               :userLoginPassword @login-password
                               :passwords []})
          (reset! logged-in true))))
    (reset! error-message "All fields must be filled in")))

(defn add-new-password
  "This function adds a new password in to the front end user atom,
   to be saved to csv prior to exiting."
  [e form-pName form-pContent form-pNotes error-message]
  (.preventDefault e)
  (let [new-password
        {:pName form-pName
         :pContent form-pContent
         :pNotes form-pNotes}]
    (if (and (seq form-pName) (seq form-pContent))
      (do
        (reset! error-message "")
        (swap! user-state update :passwords conj new-password);swap in the new password
        (reset! show-add-form false)
        #_(reset! error-message "New password entered!"));change color later
      (reset! error-message "All fields must be filled in"))))

(defn update-password [updated-password]
  (swap! user-state update-in [:passwords]
         (fn [passwords]
           (mapv (fn [password]
                   (if (= (:pName updated-password) (:pName password))
                     updated-password
                     password))
                 passwords)))
  (reset! editing-password nil))

(defn remove-a-password
  [pw-to-remove]
  (let [pw-string (:pName pw-to-remove)]
  (swap! user-state update-in
         [:passwords]
         (fn [passwords]
           (let [updated-passwords (remove #(= (:pName %) pw-string) passwords)]
             updated-passwords)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
                                        ;              JS Interop             ;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn copy-text-to-clipboard 
  "Copies text to clipboard via js interop.
      See -> clienthelpers.js"
  [pContent]
  (let [textarea (js/document.createElement "textarea")]
    (set! (.-value textarea) pContent)
    (js/document.body.appendChild textarea)
    (.select textarea)
    (js/document.execCommand "copy")
    (js/document.body.removeChild textarea)))

(defn download-csv 
  "Presents the user the generated csv for download in the browser window."
  [csv-content filename]
  (let [blob (js/Blob. #js [csv-content] #js {:type "text/csv;charset=utf-8;"})
        link (js/document.createElement "a")]
    (set! (.-href link) (js/URL.createObjectURL blob))
    (set! (.-download link) filename)
    (.appendChild js/document.body link)
    (.click link)
    (.removeChild js/document.body link)))