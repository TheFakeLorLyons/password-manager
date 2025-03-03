(ns LPM.cljs.pwhome
  (:require [reagent.core :as r]
            [LPM.cljs.helpers :as help]
            [LPM.cljs.generation :as gen]
            [LPM.cljs.editing :as edit]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
                                        ;           If logged-in              ;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn current-time []
  (.toLocaleString (js/Date.)))

(defn rainbow-export []
  [:div.rainbow-text
    [:div {:style {:color  "#290c35"}}
     "Export..."] 
   "Encrypted 0_0"])

(defn encrypted-export-component []
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

(defn unencrypted-export-component []
  (let [export-success (r/atom false)]
    (fn []
      [:div.export-container
       [:button {:on-click (fn []
                             (help/export-csv
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
        [unencrypted-export-component]
        [encrypted-export-component]])]
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

(defn edit-pw-component [current-password updated-password]
  (fn []
    [:button
     {:id "edit-pw-button"
      :on-click (fn [] 
                  (reset! help/editing-password true) 
                  (reset! updated-password current-password))}
     "Edit"]))

(defn plus-sign-component []
  (let [click-handler
        (fn [] (reset! help/show-add-form true)
          (reset! help/editing-password false))]
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
   (when (not @help/editing-password)
    [plus-sign-component])])

(defn standard-pw-list-view [updated-password]
  [:ul
   (doall
    (map-indexed
     (fn [index password]
       ^{:key index}
       [:li.password-list {:style {:list-style-type "numbered"
                                   :border-bottom ".5pt solid #b5b8d39d"}}
        [:div
         "|-------ID-------:" (:id password)]
        "|-----Name-----: " (:pName password)
        [:div.pw-list-options
         "|-PW Content-: " (:pContent password)
         [:div.pw-list-buttons
          [edit-pw-component password updated-password]
          [copy-pw-component (:pContent password)]
          [delete-pw-component password]]]
        "|-----Notes-----: " (:pNotes password)])
     (:passwords @help/user-state)))])

(defn logged-in-view []
  (let [updated-password (r/atom "")]
    (fn []
      (let [profile-name (:userProfileName @help/user-state)
            passwords (:passwords @help/user-state)]
        [:div.main-container
         [heading-box]
         [:div
          (when (not @help/show-add-form)
            [greeting profile-name])
          (when @help/show-add-form
            [gen/generation-form-box])
          (if @help/editing-password
            [edit/editing-pw-view updated-password]
            (when (not @help/show-add-form)
              [standard-pw-list-view updated-password]))
          (when (and (not @help/show-add-form) (empty? passwords))
            [:div
             "You have no passwords yet"])]]))))