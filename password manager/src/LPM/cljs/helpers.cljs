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

(defn alternative-read-file
  "Process the csv data"
  [file]
  (let [reader (js/FileReader.)]
    (set! (.-onload reader)
          (fn [event]
            (let [csv-data (-> (.-target event) .-result)]
              (js/console.log csv-data))))
    (.readAsText reader file)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
                                        ;             HTTP Helpers            ;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def user-state (r/atom {:userProfileName nil
                         :userLoginPassword nil
                         :passwords []}))

(def logged-in (r/atom false));turn back to false to get normal operation

(def show-add-form (r/atom false)); true to speed up to generation

(defn logout []
  (reset! user-state {:userProfileName nil
                      :userLoginPassword nil
                      :passwords []})
  (reset! logged-in false))

(defn valid-login? [profile-name login-password]
  false) ;L->Placeholder for actual implementation

(defn concatenate-passwords [passwords]
  (->> passwords
       (map (fn [password]
              (str "Name: " (:pName password)
                   ", Content: " (:pContent password)
                   ", Notes: " (:pNotes password))))
       (str/join "\n")))


(defn create-account [profile-name login-password]
  (js/console.log "Attempting to create account for:" profile-name)
  (ajax/POST "http://localhost:3000/create-account"
    {:params {:userProfileName @profile-name
              :userLoginPassword @login-password}
     :headers {"Content-Type" "application/json"}
     :format :json
     :response-format :json
     :handler (fn [response]
                (js/console.log "Account created:" response)
                (swap! user-state assoc :userProfileName @profile-name)
                (swap! user-state assoc :userLoginPassword @login-password)
                (swap! user-state assoc :passwords [])
                (js/console.log "FE userProfileName: " (get @user-state [:userProfileName]))
                (js/console.log "FE userLoginPassword: " (get @user-state [:userLoginPassword]))
                (js/console.log "FE passwords: " (get @user-state [:passwords]))
                (reset! logged-in true))
     :error-handler (fn [error]
                      (js/console.error "Failed to create account:" error))}))

(defn add-pw-request [profile-name login-password form-pName form-pContent form-pNotes]
  (let [new-password ;lots of extra detail in here to trim later
        {:pName form-pName
         :pContent form-pContent
         :pNotes form-pNotes}
        current-passwords (get-in @user-state [:users profile-name :passwords])]
    (ajax/POST "http://localhost:3000/add-a-new-password"
      {:params {:userProfileName profile-name
                :userLoginPassword login-password
                :passwords (conj current-passwords new-password)}
       :headers {"Content-Type" "application/json"}
       :format :json
       :response-format :json
       :handler (fn [response]
                  (js/console.log "Added a new password:" response) 
                  (swap! user-state update :passwords conj new-password)
                  (js/console.log "add PW CHECK: " (get @user-state :passwords))
                  (reset! show-add-form false))
       :error-handler (fn [error]
                        (js/console.error "Failed to add password:" error))})))

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
  (let [url (str "http://localhost:3000/generate-a-password?size=" size)]
    (js/Promise. (fn [resolve reject]
                   (ajax/GET url
                     {:response-format (ajax/json-response-format {:keywords? true})
                      :handler (fn [response]
                                 (resolve (:password response)))
                      :error-handler (fn [error]
                                       (reject error))})))))

(defn request-existing-csv []
  (js/console.log "attempting to read csv:" @csv-content)
  (ajax/POST "http://localhost:3000/request-existing-csv"
    {:params @csv-content
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
                       (map
                        (fn [pw]
                          (let [pName (-> pw (get "pName"))
                                pContent (-> pw (get "pContent"))
                                pNotes (-> pw (get "pNotes"))]
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
                      (js/console.error "Failed obtain user profile:" error))}))

(defn password-to-plain-object [password]
  (js->clj
   (js/JSON.parse
    (js/JSON.stringify
     (clj->js password)))))

(defn save-current-session [callback]
    (let [user-profile-name @(get @user-state :userProfileName)
          user-login-password @(get @user-state :userLoginPassword)
          passwords  (mapv #(update-vals % deref) (get @user-state :passwords))]
      (js/console.log "Attempting to save csv!" callback)
      (js/console.log "FE userProfileName: " (get @user-state :userProfileName))
      (js/console.log "FE userLoginPassword: " (get @user-state :userLoginPassword))
      (js/console.log "FE passwords: " (get @user-state :passwords))
      (js/console.log "newvalname: " user-profile-name)
      (js/console.log "newvalpw: " user-login-password)
      (js/console.log "newvalnotes: " passwords)
      (ajax/POST "http://localhost:3000/save-current-session"
        {:params {:userProfileName user-profile-name
                  :userLoginPassword user-login-password
                  :passwords  passwords};just added concatonate passwords now it is a string 
         :headers {"Content-Type" "text/csv"};but it is not true it is not reading right
         :format :json
         :handler (fn [response]
                    (js/console.log "Saved the session to csv!" response)
                    (callback response))
         :error-handler (fn [error]
                          (js/console.error "Failed to export csv:" error))})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
                                        ;             HTML Helpers            ;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn handle-login-submission [e profile-name login-password login error-message]
  (.preventDefault e)
  (if (and profile-name login-password (seq @profile-name) (seq @login-password))
    (do
      (println "Debug for readcsvonlogin: profile-name =" @profile-name)
      (println "Debug for readcsvonlogin: login-password =" @login-password)
      (println "Debug for readcsvonlogin: login =" login)
      (.preventDefault e))
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
            (request-existing-csv))  ; Make API request
          100)
          (reset! logged-in true))
        (do

          (swap! user-state assoc :userProfileName profile-name
                                   :userLoginPassword login-password
                                   :passwords [])
          (reset! logged-in true)
          (println "finl: profile-name =" (get @user-state :userProfileName))
          (println "finl: login-password =" (get @user-state :userLoginPassword))
          (println "finl: login =" (get @user-state :passwords))
          #_(reset! error-message "Account created successfully"))))
    (reset! error-message "All fields must be filled in")))

(defn fetch-data []
  (ajax/GET "/api/data"
    {:handler (fn [response]
                (println "Data received" response))}))
;deprecated and likely superfluous without multiple profiles
(defn handle-add-new-password-submission [e form-pName form-pContent form-pNotes error-message]
  (.preventDefault e)
  (let [profile-name (get-in @user-state [:userProfileName])
        login-password (get-in @user-state [:userLoginPassword])]
    (if (and (seq @form-pName) (seq @form-pContent))
      (do
        (reset! error-message "")
        (add-pw-request profile-name login-password @form-pName @form-pContent @form-pNotes)
        #_(reset! error-message "New password entered!"))
      (reset! error-message "All fields must be filled in"))))

(defn new-password-func [e form-pName form-pContent form-pNotes error-message]
  (.preventDefault e)
    (let [new-password
          {:pName form-pName
           :pContent form-pContent
           :pNotes form-pNotes}]
        (if (and (seq @form-pName) (seq @form-pContent))
          (do
            (reset! error-message "")
            (swap! user-state update :passwords conj new-password);swap in the new password
            (js/console.log "add PW CHECK: " (get @user-state :passwords))
            (reset! show-add-form false)
            #_(reset! error-message "New password entered!"))
          (reset! error-message "All fields must be filled in"))))

(defn copy-text-to-clipboard [pContent]
  (let [textarea (js/document.createElement "textarea")]
    (set! (.-value textarea) pContent)
    (js/document.body.appendChild textarea)
    (js/console.log "Copying text to clipboard...")
    (.select textarea)
    (js/document.execCommand "copy")
    (js/document.body.removeChild textarea)
    (js/console.log "Text copied to clipboard!")))

(defn download-csv [csv-content filename]
  (let [blob (js/Blob. #js [csv-content] #js {:type "text/csv;charset=utf-8;"})
        link (js/document.createElement "a")]
    (set! (.-href link) (js/URL.createObjectURL blob))
    (set! (.-download link) filename)
    (.appendChild js/document.body link)
    (.click link)
    (.removeChild js/document.body link)))