(ns LPM.cljs.core
  (:require [reagent.core :as r]
            [reagent.dom :as rdom]
            [LPM.cljs.helpers :as help]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
                                        ;               Static                ;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn back-button []
  (let [click-handler
        (fn [] (reset! help/show-add-form false))]
    [:div.back-button-container
     [:input {:type "button"
              :id "back-button"
              :value "<<"
              :on-click click-handler}]]))

(defn right-generation-column [form-numChar form-numUpper form-perSpaces form-perSym]
    (fn []
      [:form.generation-input-field-container
       {:style {:align-items "center"
                :transform "translate(-11vw, 0vh)"}}
       [:h3 "Complexity Modifiers"]
       [:input {:style {:width "30%"}
                :type "text"
                :id "numCharField"
                :name "number-of-characters"
                :placeholder "Total num chars"
                :required false
                :value @form-numChar
                :on-change #(let [new-value (-> % .-target .-value)]
                                 (when (re-matches #"\d*" new-value)
                                   (reset! form-numChar new-value)))}]
       [:input {:style {:width "30%"}
                :type "text"
                :id "numUpperField"
                :name "number-of-upper-case"
                :placeholder "% uppercase"
                :required false
                :value @form-numUpper
                :on-change #(let [new-value (-> % .-target .-value)]
                                 (when (re-matches #"\d*" new-value)
                                   (reset! form-numUpper new-value)))}]
       [:input {:style {:width "30%"}
                :type "text"
                :id "numSpaceField"
                :name "number-of-spaces"
                :placeholder "% spaces"
                :required false
                :value @form-perSpaces
                :on-change #(let [new-value (-> % .-target .-value)]
                                 (when (re-matches #"\d*" new-value)
                                   (reset! form-perSpaces new-value)))}]
       [:input {:style {:width "30%"}
                :type "text"
                :id "perSymField"
                :name "number-of-symbols"
                :placeholder "% symbols"
                :required false
                :value @form-perSym
                :on-change #(let [new-value (-> % .-target .-value)]
                                 (when (re-matches #"\d*" new-value)
                                   (reset! form-perSym new-value)))}]]))

(defn add-a-new-password-form [password-name password-content password-notes]
  (let [error-message (r/atom "")]
    (fn []
      [:form.generation-input-field-container ;change this from login container?
       [:h3 "Details"]
       [:input {:type "text"
                :id "newPasswordNameField"
                :name "password-name"
                :placeholder "New Password Name"
                :required true
                :value @password-name
                :on-change #(reset! password-name (-> % .-target .-value))}]
       [:input {:type "text"
                :id "passwordContentField"
                :name "password-content"
                :placeholder "New Password"
                :required true
                :value @password-content
                :on-change #(reset! password-content (-> % .-target .-value))}]
       [:input {:type "text"
                :id "passwordNotesField"
                :name "password-notes"
                :placeholder "Password Notes(Optional) "
                :required false
                :value @password-notes
                :on-change #(reset! password-notes (-> % .-target .-value))}]
       [:div.error-message {:class (when (seq @error-message) "visible")}
        (when (seq @error-message)
          [:p @error-message])]
       [:input {:type "submit"
                :value "Submit New Password"
                :on-click (fn [e]
                            (help/handle-add-new-password-submission e password-name password-content password-notes error-message))}]])))

(defn center-generation-box [form-numChar form-numUpper form-perSpaces form-perSym password-content]
  [:div.center-generation-table
   [:input {:type "button"
            :id "generate-pw-button"
            :value "Generate PW!"
            :on-click (fn [e]
                        (-> (help/generate-password-request @form-numChar)
                            (.then (fn [new-password]
                                     (reset! password-content new-password)))
                            (.catch (fn [error]
                                      (js/console.error "Failed to generate password:" error)))))}]])

(defn generation-form []
  (let [password-name (r/atom "")
        password-content (r/atom "")
        password-notes (r/atom "")
        form-numChar (r/atom "")
        form-numUpper (r/atom "")
        form-perSpaces (r/atom "")
        form-perSym (r/atom "")]
    [:div.back-button-container
     [back-button]
     [:div.pw-generation-header 
      [:h2 "Add a new password"]
      [:h3 "Manually enter your own password, or generate one based on the
           right side properties."]
      [:div.generation-container
       [add-a-new-password-form password-name password-content password-notes]
       [center-generation-box form-numChar form-numUpper form-perSpaces form-perSym password-content]
       [right-generation-column form-numChar form-numUpper form-perSpaces form-perSym]]]]))

(defn generation-form-box []
  (let [hidden-container false]
    (fn []
      (generation-form))))

(defn plus-sign-component []
  (let [click-handler
        (fn [] (reset! help/show-add-form true))]
    [:div.add-button-container
     [:input {:type "button"
              :id "plus-button"
              :value "+"
              :on-click click-handler}]]))

(defn delete-pw-component [profile-name pName]
  (let [click-handler
        (fn [] (help/remove-pw-request profile-name pName))]
    [:div.remove-button-container
     [:input {:type "button"
              :id "delete-pw-component"
              :value "X"
              :style {:padding-top "0px"
                      :padding-right "4px"
                      :font-style "bold"
                      :color "#781848"
                      :cursor "pointer"
                      :transform "translate(1vw, -0.2vh)"}
              :on-click click-handler}]]))

(defn heading-box []
  (if @help/logged-in
    [:div.logged-in-heading-container
     #_{:style
      {:opacity (if (:loggedIn @help/logged-in) 1 0)}};fades in login effect
     [:h2 "Lor's Password Manager"]
     [:button {:id "logout-button"
               :on-click (fn []
                           (help/logout))} "Logout"]]
    [:div.heading-container
     [:h1 "Lor's Password Manager"]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
                                        ;             Login-Page              ;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn current-time []
  (.toLocaleString (js/Date.)))

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
       [:button {:on-click #(-> (js/document.getElementById "directory-path") .click)}
        "Select Directory"]])))

(defn name-and-password-input [on-login]
  (let [profile-name (r/atom "")
        login-password (r/atom "")
        error-message (r/atom "")]
    (fn []
      [:form.input-field-container
       [:input {:type "text"
                :id "profileName"
                :name "profile-name"
                :placeholder "Profile Name"
                :required true
                :value @profile-name
                :on-change #(reset! profile-name (-> % .-target .-value))}]
       [:input {:type "password"
                :id "password"
                :name "password"
                :placeholder "Password"
                :required true
                :value @login-password
                :on-change #(reset! login-password (-> % .-target .-value))}]
       [:div.error-message {:class (when (seq @error-message) "visible")}
        (when (seq @error-message)
          [:p @error-message])]
       [:input {:type "submit"
                :value "Login"
                :on-click (fn [e]
                            (help/handle-login-submission e profile-name login-password on-login error-message))}]
       [:input {:type "button"
                :id "caccount-button"
                :value "New?"
                :on-click (fn [e]
                            (help/handle-login-submission e profile-name login-password nil error-message))}]])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
                                        ;           If logged-in              ;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn logged-in-view [profile-name]
  (let [passwords (@help/user-state :passwords)]
    [:div.main-container
     [heading-box]
     [:div
      (when (not @help/show-add-form)
        [:div
         [:h2 (str "Hello " profile-name ", you logged in at " (current-time))]
         [plus-sign-component]])
      (when @help/show-add-form
        [generation-form-box])
      (when (and (not @help/show-add-form) (empty? passwords))
        [:div
         "You have no passwords yet"])
      (when (not @help/show-add-form)
        [:ul
         (for [{:keys [pName pContent pNotes]} passwords]
           ^{:key pName}
           [:li.password-list (str "Name: " pName ", Content: " pContent ", Notes: " pNotes)
            [delete-pw-component (:userProfileName @help/user-state ) pName]])])]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
                                        ;                 Frame               ;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn main-page-input-container []
  (let [selected-file (r/atom nil)
        error-message (r/atom "")]
    (fn []
      (if  @help/logged-in
       [logged-in-view  (:current-user @help/user-state)]
        [:div.main-container
         [heading-box]
         [:h2 "Login"]
         [directory-box (fn [file]
                          (reset! selected-file file))]
         [name-and-password-input
          (fn [profile-name password]
            (if (or (empty? profile-name) (empty? password))
              (reset! error-message "Please fill in both fields.")
              (if (and @selected-file (help/valid-login? profile-name password));authenticate user here
                (do
                  (swap! @help/user-state assoc :current-user profile-name)
                  (reset! @help/logged-in {:loggedIn false})
                  (help/read-file @selected-file)) ;Where reading a file will take place
                (reset! error-message "Invalid profile-name or password"))))]
         (when (not @help/logged-in)
           [:div.error-message @error-message])]))))

(defn login-page []
  (main-page-input-container))

(defn ^:dev/after-load start []
  (rdom/render [login-page]
               (.getElementById js/document "app")))

(start)


(comment
  ;TODOS
  ;On-Login needs to require a path
  ;Handle Validation/Authentification
  ;Addpassword needs some methods
  ;Encryption/Decrpytion
  ;Update Passwords
  ;Delete Passwords
  ;Sort Passwords
  ;Group Passwords
  ;Password Hotkeys
  ;Add default values to the add pw page
  )