(ns LPM.cljs.pwhome
  (:require [reagent.core :as r]
            [LPM.cljs.helpers :as help]
            [LPM.cljs.generation :as gen]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
                                        ;           If logged-in              ;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn current-time []
  (.toLocaleString (js/Date.)))

(defn rainbow-export []
  [:div.rainbow-text
    [:div {:style {:color  "#290c35"}}
     (str "Export...")] 
   (str "Encrypted 0_0")])

(defn export-encrypted-component []
  (let [export-success (r/atom false)]
    (fn []
      [:div.export-container
       [:button {:on-click (fn []
                             (help/export-encrypted-csv
                              (fn [csv-content]
                                (help/download-csv csv-content "encrypted.csv")
                                (reset! export-success true)
                                (js/setTimeout #(reset! export-success false) 5000))))}
        [rainbow-export]]
       (when @export-success
         [:div {:style {:color "#66ff00"
                        :transform "translate(5vh, 0vh)"
                        :text-weight "bold"}}])])))

(defn save-session-component []
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
        [save-session-component]
        [export-encrypted-component]])]
    [:div.heading-container
     [:h1 "Lor's Password Manager"]]))

(defn delete-pw-component [password]
  (let [click-handler
        #(help/remove-a-password password)]
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

(defn edit-pw-component [editing-password]
  (let [text (r/atom editing-password)]
    (fn []
      [:button
       {:id "edit-pw-button"
        :on-click #(reset! help/editing-password editing-password)}
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

(defn greeting [profile-name]
  [:div
   [:h2 {:style {:text-align "center"}}
    (str "Hello " profile-name ", you logged in at " (current-time))];@=newuser
   [:div {:style {:border-bottom "1pt solid #ede9f6"
                  :width "max"
                  :align-self "center"}}]
   [plus-sign-component]])

(defn standard-pw-list-view []
  [:ul
   (doall
    (map-indexed
     (fn [index password]
       ^{:key index}
       [:li.password-list {:style {:list-style-type "numbered"
                                   :border-bottom ".5pt solid #b5b8d39d"}}
        "|-----Name-----: " (get password :pName);
        [:div.pw-list-options
         "|-PW Content-: " (get password :pContent);
         [:div.pw-list-buttons
          [edit-pw-component password]
          [copy-pw-component (:pContent password)]
          [delete-pw-component password]]]
        "|-----Notes-----: " (get password :pNotes)])
     (:passwords @help/user-state)))]);taking the passwords from user state and iterating the above

(defn editing-pw-view []
  (fn []
    [:ul
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
        "Cancel"]]]]));displays the specific password to be edited

(defn logged-in-view []
  (fn []
    (let [user-state @help/user-state
          profile-name (:userProfileName user-state)
          passwords (get user-state :passwords)]
      [:div.main-container
       [heading-box]
       [:div
        (when (not @help/show-add-form)
          [greeting profile-name])
        (when @help/show-add-form
          [gen/generation-form-box])
        (if @help/editing-password
          [editing-pw-view]
          (when (not @help/show-add-form)
            [standard-pw-list-view]))
        (when (and (not @help/show-add-form) (empty? passwords))
          [:div
           "You have no passwords yet"])]])))