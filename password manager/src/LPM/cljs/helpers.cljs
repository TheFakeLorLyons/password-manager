(ns LPM.cljs.helpers
  (:require [ajax.core :as ajax]
            [reagent.core :as r]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
                                        ;              cljs-io                ;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn read-file
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
                         :passwords []
                         :login-success false}))

(defn logout []
  (reset! user-state {:userProfileName nil
                      :userLoginPassword nil
                      :passwords []
                      :login-success false}))

(defn valid-login? [profile-name login-password]
  false) ; Placeholder for actual implementation

(defn create-account [profile-name login-password]
  (ajax/POST "http://localhost:3000/create-account"
    {:params {:userProfileName profile-name
              :userLoginPassword login-password}
     :headers {"Content-Type" "application/json"}
     :format :json
     :response-format :json
     :handler (fn [response]
                (js/console.log "Account created:" response)
                (swap! user-state assoc :userProfileName profile-name)
                (swap! user-state assoc :userLoginPassword login-password)
                (swap! user-state assoc :passwords [])
                (swap! user-state assoc :login-success true))
     :error-handler (fn [error]
                      (js/console.error "Failed to create account:" error))}))

(defn add-pw-request [profile-name login-password form-pName form-pContent form-pNotes]
  (let [new-password ;lots of extra detail in here to trim later
        {:pName form-pName ;potentially decouple login-success from user to just pass user object
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
                  (swap! user-state update :passwords conj new-password))
       :error-handler (fn [error]
                        (js/console.error "Failed to add password:" error))})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
                                        ;             HTML Helpers            ;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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

(defn handle-login-submission [e profile-name login-password on-login error-message]
  (.preventDefault e)
  (if (and (seq @profile-name) (seq @login-password))
    (do
      (reset! error-message "")
      (if (:login-success @user-state)
        (on-login @profile-name @login-password);implemented
        (do
          (create-account @profile-name @login-password)
          #_(reset! error-message "Account created successfully"))))
    (reset! error-message "All fields must be filled in")))

(defn fetch-data []
  (ajax/GET "/api/data"
    {:handler (fn [response]
                (println "Data received" response))}))