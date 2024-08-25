(ns LPM.cljs.core
  (:require [reagent.core :as r]
            [reagent.dom :as rdom]
            [LPM.cljs.helpers :as help]
            [LPM.cljs.pwhome :as home]
            [LPM.cljs.login :as lgn]
            [LPM.cljs.initialsetup :as isup]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
                                        ;                 Frame               ;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn main-page-input-container []
  (let [selected-file (r/atom nil)
        error-message (r/atom "")]
    (fn []
      [:div
       (if @help/logged-in
         [home/logged-in-view]
         [:div.main-container {:style {:margin-bottom (if @help/logged-in "5vh" "10vh")}}
          [home/heading-box]
          [:h2 {:style {:margin-bottom "2vh" :margin-top "3vh"}} "Login"]
          [lgn/directory-box
           (fn [file]
             (reset! selected-file file))]
          [lgn/name-and-password-input selected-file
           (fn [profile-name password]
             (if (or (empty? profile-name) (empty? password))
               (reset! error-message "Please fill in both fields.")
               (if (and @selected-file (and profile-name password))
                 (do
                   (swap! @help/user-state assoc :current-user profile-name)
                   (reset! @help/logged-in {:loggedIn false}))
                 (reset! error-message "Invalid profile-name or password"))))]
          (when (not @help/logged-in)
            [:div.error-message @error-message])])])))

(defn login-page []
  (let [loading? (r/atom true)
        setup-complete? (r/atom false)]

    (r/create-class
     {:component-did-mount
      (fn []
        (help/check-setup
         (fn [status]
           (reset! setup-complete? status)
           (reset! loading? false))))

     :reagent-render
     (fn []
       (cond
         @loading?
         [:div.loadinggg
          [:img {:src "/assets/loading.gif"}]
          [:p "Loading..."]]

         (not @setup-complete?)
         [isup/key-setup-component]

         :else
         [main-page-input-container]))})))

(defn ^:dev/after-load start []
  (rdom/render [login-page]
               (.getElementById js/document "app")))

(start)