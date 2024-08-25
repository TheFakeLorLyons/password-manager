(ns LPM.cljs.initialsetup
  (:require [reagent.core :as r]
            [LPM.cljs.helpers :as help]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
                                        ;         Buttons and Utility         ;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn validate-key [key]
  (try
    (and (string? key)
         (= 64 (count key))
         (re-matches #"[0-9a-fA-F]+" key))
    (catch js/Error e
    (js/console.log "Caught exception:" (.-message e))
    false)))

(defn back-button []
  (let [click-handler
        (fn []
          (let [history (rest (:history @help/key-state))]
            (when (seq  history)
              (swap! help/key-state assoc :mode (first history))
              (swap! help/key-state assoc :history history))))]
    [:div.input-back-button-container
     [:input {:type "button"
              :id "back-button"
              :value "<<"
              :on-click click-handler}]]))

(defn copy-pw-component [key]
  (let [key-text (r/atom key)]
    (fn []
      [:button
       {:id "copy-pw-button"
        :on-click #(help/copy-text-to-clipboard @key-text)}
       "[]"])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
                                        ;            Key-Generation           ;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn generate-keys-component []
  (let [keys (r/atom nil)]
    (r/create-class
     {:component-did-mount
      (fn []
        (-> (help/generate-keys)
            (.then #(reset! keys %))
            (.catch #(js/console.error "Error generating keys:" %))))

      :reagent-render
      (fn []
        (let [secret (:secret-key @keys)
              public (:public-key @keys)]
          [:div
           [back-button]
           (if @keys
             [:div
              (js/console.log secret " and " public "gendiv" @keys)
              [:h2 "Generated Keys"]
              [:div {:style {:display "flex"}}
               [:p "Secret Key: " secret]
               [copy-pw-component secret]]
              [:div {:style {:display "flex"}}
               [:p "Public Key: " public]
               [copy-pw-component public]]
              [:button {:on-click #(do (help/save-keys @keys)
                                       (help/mark-setup-complete @keys)
                                       (swap! help/key-state assoc :mode :complete
                                              :history (conj (:history @help/key-state) :generate)))}
               "Save and Complete Setup"]]
             [:div.loadinggg
              [:img {:src "../resources/public/assets/loading.gif"}]
              [:p "Generating keys..."]])]))})))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
                                        ;          Main Setup Component       ;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn key-setup-component []
  (fn []
    [:div.setup-main-container
     (case (:mode @help/key-state)
       :choose
       [:div.setup-container
        [:h2.setup-heading "Key Setup"]
        [:p "Choose how you want to set up your keys:"]
        [:button {:on-click #(swap! help/key-state assoc :mode :generate
                                    :history (conj (:history @help/key-state) :choose))} "Generate Random Keys"]
        [:button {:on-click #(swap! help/key-state assoc :mode :manual
                                    :history (conj (:history @help/key-state) :choose))} "Enter My Own Keys"]]

       :generate
       [generate-keys-component]

       :manual
       [:div
        [back-button]
        [:h2 "Enter Your Keys"]
        [:div
         [:label "Secret Key: "]
         [:input {:type "text"
                  :value (:secret-key @help/key-state)
                  :on-change #(swap! help/key-state assoc :secret-key (-> % .-target .-value))}]]
        [:div
         [:label "Public Key: "]
         [:input {:type "text"
                  :value (:public-key @help/key-state)
                  :on-change #(swap! help/key-state assoc :public-key (-> % .-target .-value))}]]
        (when (:error @help/key-state)
          [:p.error (:error @help/key-state)])
        [:button {:on-click #(if (and (validate-key (:secret-key @help/key-state))
                                      (validate-key (:public-key @help/key-state)))
                               (let [keys {:secret-key (:secret-key @help/key-state)
                                           :public-key (:public-key @help/key-state)}]
                                 (help/save-keys keys)
                                 (help/mark-setup-complete keys)
                                 (swap! help/key-state assoc :mode :complete
                                        :history (conj (:history @help/key-state) :manual)))
                               (swap! help/key-state assoc :error "Invalid key format. Keys should be 64-character hexadecimal strings."))}
         "Save and Complete Setup"]]

       :complete
       [:div
        [:h2 "Setup Complete"]
        [:p "Your keys have been saved and the setup is complete."]
        [:button {:on-click #(js/window.location.reload)} "Restart Application"]])]))