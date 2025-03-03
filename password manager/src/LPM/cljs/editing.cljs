(ns LPM.cljs.editing
  (:require [reagent.core :as r]
            [LPM.cljs.helpers :as help]))

(defn editing-pw-view [password]
  (let [form-numChar (r/atom (str (count (:pContent @password))))]
    (fn []
      [:ul
       [:li {:style {:list-style-type "none"
                     :border-bottom ".5pt solid #b5b8d39d"}}

        [:p {:for "id"} "Password ID: " (:id @password)]

        [:label {:for "pName"} "Name: "]
        [:input {:class "edit-label"
                 :type "text"
                 :value (:pName @password)
                 :on-change #(swap! password assoc :pName (-> % .-target .-value))}]

        [:label {:for "pName"} "Password: "]
        [:div {:style {:display "flex"
                       :align-items "center"
                       :gap "10px"}}
         [:input {:class "edit-label"
                  :type "text"
                  :value (:pContent @password)
                  :on-change #(swap! password assoc :pContent (-> % .-target .-value))
                  :style {:width "80%"}}]
         [:input {:type "button"
                  :id "generate-pw-button"
                  :value "Generate"
                  :on-click (fn [e]
                              (-> (help/generate-password-request @form-numChar)
                                  (.then (fn [new-password]
                                           (swap! password assoc :pContent new-password)))
                                  (.catch (fn [error]
                                            (js/console.error "Failed to generate password:" error)))))}]

         [:label {:for "pName"} "Size: "]
         [:input {:style {:width "15%"}
                  :type "text"
                  :id "numCharField"
                  :name "number-of-characters"
                  :value @form-numChar
                  :on-change #(let [new-value (-> % .-target .-value)]
                                (when (re-matches #"\d*" new-value)
                                  (reset! form-numChar new-value)))}]]
        
        [:label {:for "pName"} "Notes: "]
        [:input {:class "edit-label"
                 :type "text"
                 :value (:pNotes @password)
                 :on-change #(swap! password assoc :pNotes (-> % .-target .-value))}]

        [:div.edit-pw-list-buttons {:style {:margin-left "15px"
                                            :margin-bottom "10px"}}
         [:button {:style {:margin-right "10px"}
                   :on-click #(help/update-password @password)}
          "Save"]
         [:button
          {:on-click #(reset! help/editing-password false)}
          "Cancel"]]]])))