(ns LPM.clj.user)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
                                        ;             User profiles           ;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def current-user (atom
            {:users
             {"profile" {:userProfileName "Admin User"
                         :userLoginPassword "password123"
                         :passwords [{:pName "example"
                                      :pContent "exampleContent"
                                      :pNotes "Example note"}]}}}))

(defn on-create-account
  [profile-name login-password]
  (swap! current-user update-in [:users profile-name]
         (fn [_]
           {:userProfileName profile-name
            :userLoginPassword login-password
            :passwords []})))

(defn add-new-password
  [profile-name login-password new-password]
  (swap! current-user update-in [:users profile-name]
         (fn [user]
           (if user
             (update user :passwords conj new-password)
             {:userProfileName profile-name
              :userLoginPassword login-password
              :passwords [new-password]}))))

(defn on-login
  [profile-name login-password]
  ;get logic for old csv
  ;unencrypt logic 
  (swap! current-user update-in [:users profile-name] (fn [_]
                                                   {:userProfileName profile-name
                                                    :userLoginPassword login-password
                                                    :passwords []})))
