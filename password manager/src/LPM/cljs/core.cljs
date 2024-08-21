(ns LPM.cljs.core
  (:require [reagent.core :as r]
            [reagent.dom :as rdom]
            [LPM.cljs.helpers :as help]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
                                        ; Add a PW / PW Generation Components ;
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
      (js/console.log "rightform1-numChar value:", @form-numChar)
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
                                (reset! form-numChar new-value))
                              (js/console.log "rightform2-numChar value:", @form-numChar))}]
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
                            (help/new-password-func e @password-name @password-content @password-notes error-message))}]])))

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
       (fn []
         (js/console.log "genformform-numChar value:", @form-numChar)
         [:div.back-button-container
          [back-button]
          [:div.pw-generation-header
           [:h2 "Add a new password"]
           [:h3 "Manually enter your own password, or generate one based on the
           right side properties."]
           [:div.generation-container
            [add-a-new-password-form password-name password-content password-notes]
            [center-generation-box form-numChar form-numUpper form-perSpaces form-perSym password-content]
            [right-generation-column form-numChar form-numUpper form-perSpaces form-perSym]]]])))

(defn generation-form-box []
  (let [hidden-container false]
    (fn []
      (generation-form))))

(defn save-session-component [selected-export]
  (let [export-success (r/atom false)]
      (fn []
        [:div.export-container
         [:button {:on-click (fn []
                               (help/save-current-session
                               (fn [csv-content]
                                 (help/download-csv csv-content "passwords.csv")
                                 (reset! export-success true)
                                 (js/setTimeout #(reset! export-success false) 5000))))}
          "Export CSV"]
         (when @export-success
           [:div {:style {:color "#66ff00" 
                          :transform "translate(5vh, 0vh)"
                          :text-weight "bold"}} "Export Successful"])])))

(defn heading-box []
  (if @help/logged-in
    [:div
     [:div.logged-in-heading-container
      #_{:style
         {:opacity (if (:loggedIn @help/logged-in) 1 0)}};fades in login effect
      [:h2 "Lor's Password Manager"]
      [:button {:id "logout-button"
                :on-click (fn []
                            (help/logout))} "Logout"]]
     (when (not @help/show-add-form)
       [:div.logged-in-io-buttons
        [save-session-component]])]
    [:div.heading-container
     [:h1 "Lor's Password Manager"]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
                                        ;             Login-Page              ;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn current-time []
  (.toLocaleString (js/Date.)))

(defn directory-box [selected-file]
  (let [file (r/atom selected-file)
        file-name (r/atom "No file selected")]
    (fn []
      [:div.directory-container
       [:input {:type "file"
                :id "directory-path"
                :name "directory-path"
                :placeholder "Directory Path"
                :accept ".csv"
                :style { :display "none"}
                :on-change (fn [e]
                             (let [changed-file (-> e .-target .-files (aget 0))]
                               (when changed-file
                                 (reset! file changed-file)
                                 (reset! file-name (.-name changed-file))
                                 (println "Debug: File selected in directory-box:" @file)
                                 (selected-file changed-file))))}]
       [:div.selected-csv-path-container
        (str "Selected File: " @file-name)]
       [:button {:on-click #(-> (js/document.getElementById "directory-path") .click)}
        "Select Directory"]])))

(defn name-and-password-input [selected-file]
  (let [profile-name (r/atom "")
        login-password (r/atom "")
        error-message (r/atom "")
        login (r/atom false)]
    (fn []
      [:form.login-field-container
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
                            (.preventDefault e)
                            (reset! login true)
                            (println "Debug: File selected in nameinputcont callback:" @selected-file)
                            (when (some? @selected-file)
                               (help/handle-file-selection @selected-file))
                            (help/handle-login-submission e profile-name login-password login error-message))}]
       [:input {:type "button"
                :id "caccount-button"
                :value "New?"
                :on-click (fn [e]
                            (.preventDefault e)
                            (help/handle-login-submission e profile-name login-password login error-message))}]])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
                                        ;           If logged-in              ;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn delete-pw-component [profile-name pName]
  (let [click-handler
        (fn [] (help/remove-pw-request profile-name pName))]
    [:div.remove-button-container
     [:input {:type "button"
              :id "delete-pw-button"
              :value "X"
              :on-click click-handler}]]))

(defn copy-pw-component [pContent]
  (let [text (r/atom pContent)]
    (fn []
      [:button
       {:id "copy-pw-button"
        :on-click #(help/copy-text-to-clipboard @text)}
       "[]"])))

(defn edit-pw-component [password]
  (let [text (r/atom password)]
    (fn []
      [:button
       {:id "edit-pw-button"
        :on-click #(reset! help/editing-password password)}
       "Edit"])))

(defn plus-sign-component []
  (let [click-handler
        (fn [] (reset! help/show-add-form true)
          (reset! help/editing-password nil))]
    [:div.add-button-container
     [:input {:type "button"
              :id "plus-button"
              :value "+"
              :on-click click-handler}]]))

(defn logged-in-view []
  (fn []
    (let [user-state @help/user-state
          profile-name (get user-state :userProfileName "Unknown")
          passwords (get user-state :passwords)]
      [:div.main-container
       [heading-box]
       [:div
        (when (not @help/show-add-form)
          [:div
           [:h2 {:style {:text-align "center"}}
            (str "Hello " profile-name ", you logged in at " (current-time))];@=newuser
           [:div {:style {:border-bottom "1pt solid #ede9f6"
                          :width "max"
                          :align-self "center"}}]
           [plus-sign-component]])
        (when @help/show-add-form
          [generation-form-box])
        (if @help/editing-password
          [:ul
           (js/console.log "editing pw")
           [:li.password-list {:style {:list-style-type "numbered"
                                       :border-bottom ".5pt solid #b5b8d39d"}}
            [:input {:type "text"
                     :value (:pName @help/editing-password)
                     :on-change #(swap! help/editing-password assoc :pName (-> % .-target .-value))}]

            [:input {:type "text"
                     :value (:pContent @help/editing-password)
                     :on-change #(swap! help/editing-password assoc :pContent (-> % .-target .-value))}]
            [:input {:type "text"
                     :value (:pNotes @help/editing-password)
                     :on-change #(swap! help/editing-password assoc :pNotes (-> % .-target .-value))}]
            [:div.edit-pw-list-buttons
             [:button
              {:on-click #(help/update-password @help/editing-password)}
              "Save"]
             [:button
              {:on-click #(reset! help/editing-password nil)}
              "Cancel"]]]]
          (when (not @help/show-add-form)
            [:ul
             (doall
              (map-indexed
               (fn [index password]
                 ^{:key index}
                 [:li.password-list {:style {:list-style-type "numbered"
                                             :border-bottom ".5pt solid #b5b8d39d"}}
                  "|-----Name-----: " (get password :pName);@=newuser
                  [:div.pw-list-options
                   "|-PW Content-: " (get password :pContent);@=newuser
                   [:div.pw-list-buttons
                    [edit-pw-component password]
                    [copy-pw-component (:pContent password)]
                    [delete-pw-component (:userProfileName @help/user-state) (:pName password)]]];@=newuser
                  "|-----Notes-----: " (get password :pNotes)])
               (:passwords @help/user-state)))]))
        (when (and (not @help/show-add-form) (empty? passwords))
          [:div
           "You have no passwords yet"])]])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
                                        ;                 Frame               ;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn main-page-input-container []
  (let [selected-file (r/atom nil)
        error-message (r/atom "")]
    (fn []
      (if @help/logged-in
        [logged-in-view]
        [:div.main-container {:style {:margin-bottom (if @help/logged-in "5vh" "10vh")}}
         [heading-box]
         [:h2 {:style {:margin-bottom "2vh" :margin-top "3vh"}} "Login"]
         [directory-box
          (fn [file]
            (println "Debug: File selected in main-page-input-container callback:" file)
            (reset! selected-file file))]
         [name-and-password-input selected-file
          (fn [profile-name password]
            (if (or (empty? profile-name) (empty? password))
              (reset! error-message "Please fill in both fields.")
              (do
                (println "Debug: File selected in name-and-password-input callback:" @selected-file)
                (if (and @selected-file (help/valid-login? profile-name password))
                  (do
                    (swap! @help/user-state assoc :current-user profile-name)
                    (println "csv is about to be read")
                    (reset! @help/logged-in {:loggedIn false}))
                  (reset! error-message "Invalid profile-name or password")))))]
         (when (not @help/logged-in)
           [:div.error-message @error-message])]))))

(defn login-page []
  (main-page-input-container))

(defn ^:dev/after-load start []
  (rdom/render [login-page]
               (.getElementById js/document "app")))

(start)