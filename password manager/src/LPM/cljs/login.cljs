(ns LPM.cljs.login
  (:require [reagent.core :as r]
            [reagent.dom :as rdom]
            [LPM.cljs.helpers :as help]
            [LPM.cljs.pwhome :as home]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
                                        ;             Login-Page              ;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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
                            (when (some? @selected-file)
                               (help/handle-file-selection @selected-file))
                            (help/handle-login-submission e profile-name login-password login error-message))}]
       [:input {:type "button"
                :id "caccount-button"
                :value "New?"
                :on-click (fn [e]
                            (.preventDefault e)
                            (help/handle-login-submission e profile-name login-password login error-message))}]])))
