(ns LPM.clj.user)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
                                        ;             User profiles           ;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def current-user (atom
            {:users
             {"profile" {:userProfileName nil
                         :userLoginPassword nil
                         :passwords [{:pName nil
                                      :pContent nil
                                      :pNotes nil}]}}}))

(defn on-create-account
  [profile-name login-password]
  (swap! current-user update-in [:users profile-name]
         (fn [_]
           {:userProfileName profile-name
            :userLoginPassword login-password
            :passwords []})))

(defn remove-a-password
  [profile-name pw-to-remove]
  (swap! current-user update-in
         [:users profile-name :passwords]
         (fn [passwords]
           (let [updated-passwords (remove #(= (:pName %) pw-to-remove) passwords)]
             updated-passwords))))

(defn add-new-password
  [profile-name login-password new-password]
  (swap! current-user update-in [:users profile-name]
         (fn [user]
           (if user
             (update user :passwords conj new-password)
             {:userProfileName profile-name
              :userLoginPassword login-password
              :passwords [new-password]}))))


(defn get-profile-on-login;currently not being used
  [profile-name login-password passwords]
  (swap! current-user update-in [:users profile-name]
         (fn [_]
           {:userProfileName profile-name
            :userLoginPassword login-password
            :passwords passwords})))