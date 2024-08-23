(ns LPM.cljs.generation
  (:require [reagent.core :as r]
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
                            (help/add-new-password e @password-name @password-content @password-notes error-message))}]])))

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