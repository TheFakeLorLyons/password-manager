(ns LPM.cljs.helpers
  (:require [ajax.core :as ajax]
            [reagent.core :as r]
            [clojure.string :as str]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
                                        ;              cljs-io                ;
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
                                        ;             HTTP Helpers            ;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def user-state (r/atom {:userProfileName nil
                         :userLoginPassword nil
                         :passwords []}))

(def logged-in (r/atom false))

(def show-add-form (r/atom false));true to speed up to generation

(defn logout []
  (reset! user-state {:userProfileName nil
                      :userLoginPassword nil
                      :passwords []})
  (reset! logged-in false))

(defn remove-pw-request [profile-name pName]
  (ajax/DELETE "http://localhost:3000/remove-a-password"
    {:params {:userProfileName profile-name
              :pName pName}
     :headers {"Content-Type" "application/json"}
     :format :json
     :response-format :json
     :handler (fn [response]
                (js/console.log "Removed password:" response)
                (swap! user-state update :passwords
                       (fn [passwords]
                         (remove #(= (:pName %) pName) passwords))))
     :error-handler (fn [error]
                      (js/console.error "Failed to remove password:" error))}))

(defn generate-password-request [size]
  (let [entered-size (if (seq size) size "12")]
  (js/console.log "api call to generate password:" entered-size)
  (let [url (str "http://localhost:3000/generate-a-password?size=" entered-size)]
    (js/Promise. (fn [resolve reject]
                   (ajax/GET url
                     {:response-format (ajax/json-response-format {:keywords? true})
                      :handler (fn [response]
                                 (resolve (get response :password))
                                  (js/console.log "api call to generate password:" (get response :password)))
                      :error-handler (fn [error]
                                       (reject error))}))))))

(defn request-existing-csv [profile-name login-password]
  (js/console.log "attempting to read csv:" @csv-content)
  (ajax/POST "http://localhost:3000/request-existing-csv"
    {:params {:csv-content @csv-content
              :userProfileName profile-name
              :userLoginPassword login-password}
     :headers {"Content-Type" "application/json"}
     :format :json
     :response-format :json
     :handler (fn [response]
                (js/console.log "Raw Response: " response)
                (let [profile (js->clj response :keywordize-keys true)
                      profile-name (get response "userProfileName")
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
                  (js/console.log "Logged in successfully:" profile)
                  (reset! user-state {:userProfileName profile-name
                                      :userLoginPassword login-password
                                      :passwords processed-passwords}))
                (js/console.log "userNames on obtaining profile:" (@user-state :userProfileName))
                (js/console.log "loginPassword on obtaining profile:" (@user-state :userLoginPassword))
                (js/console.log "passwords on obtaining profile:" (@user-state :passwords))
                (reset! logged-in true))
     :error-handler (fn [error]
                      (reset! logged-in false)
                      (js/console.error "Failed obtain user profile:" error))}))

(defn save-current-session [callback]
  (let [user-profile-name (get @user-state :userProfileName);@=newuser
        user-login-password (get @user-state :userLoginPassword);@=newuser
        passwords  (get @user-state :passwords)];(mapv #(update-vals % deref) (get @user-state :passwords))=newuser
    (js/console.log "Attempting to save csv!" callback)
    (js/console.log "FE userProfileName: " (get user-state :userProfileName))
    (js/console.log "FE userLoginPassword: " (get user-state :userLoginPassword))
    (js/console.log "FE passwords: " (get user-state :passwords))
    (js/console.log "newvalname: " user-profile-name)
    (js/console.log "newvalpw: " user-login-password)
    (js/console.log "newvalnotes: " passwords)
    (ajax/POST "http://localhost:3000/save-current-session"
      {:params {:userProfileName user-profile-name
                :userLoginPassword user-login-password
                :passwords  passwords}
       :headers {"Content-Type" "text/csv"}
       :format :json
       :handler (fn [response]
                  (js/console.log "Saved the session to csv!" response)
                  (callback response))
       :error-handler (fn [error]
                        (js/console.error "Failed to export csv:" error))})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
                                        ;             HTML Helpers            ;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;bools for UI components
(def editing-password (r/atom nil))

(defn update-password [updated-password]
  (swap! user-state update-in [:passwords]
         (fn [passwords]
           (mapv (fn [password]
                   (if (= (:pName updated-password) (:pName password))
                     updated-password
                     password))
                 passwords)))
  (reset! editing-password nil))

(defn handle-login-submission 
  "This function either creates a blank slate user, or draws exising user
   information from CSV using the above 'request-existing-csv' fn."
  [event profile-name login-password login error-message]
  (.preventDefault event)
  (if (and profile-name login-password (seq @profile-name) (seq @login-password))
    (do
      (println "Debug for readcsvonlogin: profile-name =" @profile-name)
      (println "Debug for readcsvonlogin: login-password =" @login-password)
      (println "Debug for readcsvonlogin: login =" login)
      (.preventDefault event))
    (do
      (reset! error-message "All fields must be filled in")
      (println "Debug: Empty fields detected")))
  (if (and (seq @profile-name) (seq @login-password))
    (do
      (reset! error-message "")

      (if @login
        (do
          (println "pre handler csv" @csv-content)
          (js/setTimeout  ; Ensure csv-content is set before making request
           (fn []
             (request-existing-csv @profile-name @login-password))  ; Make API request
           100)
          (reset! logged-in true))
        (do

          (reset! user-state  {:userProfileName @profile-name
                               :userLoginPassword @login-password
                               :passwords []})
          (reset! logged-in true)
          (println "finl: profile-name =" (get @user-state :userProfileName))
          (println "finl: login-password =" (get @user-state :userLoginPassword))
          (println "finl: login =" (get @user-state :passwords))
          #_(reset! error-message "Account created successfully"))))
    (reset! error-message "All fields must be filled in")))

(defn new-password-func 
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
        (js/console.log "add PW CHECK: " (get @user-state :passwords))
        (reset! show-add-form false)
        #_(reset! error-message "New password entered!"));Change this to a green login-success icon
      (reset! error-message "All fields must be filled in"))))

(defn remove-a-password
  [pw-to-remove]
  (let [pw-string (:pName pw-to-remove)]
  (swap! user-state update-in
         [:passwords]
         (fn [passwords]
           (let [updated-passwords (remove #(= (:pName %) pw-string) passwords)]
             updated-passwords)))))

(defn copy-text-to-clipboard 
  "Copies text to clipboard via js interop.
      See -> clienthelpers.js"
  [pContent]
  (let [textarea (js/document.createElement "textarea")]
    (set! (.-value textarea) pContent)
    (js/document.body.appendChild textarea)
    (js/console.log "Copying text to clipboard...")
    (.select textarea)
    (js/document.execCommand "copy")
    (js/document.body.removeChild textarea)
    (js/console.log "Text copied to clipboard!")))

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