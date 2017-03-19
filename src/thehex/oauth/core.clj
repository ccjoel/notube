(ns thehex.oauth.core
  (:require [thehex.oauth.lib :as oauth]
            [thehex.notube.config :as config]
            [taoensso.timbre :as log]))

(defn -main
  "FIXME"
  [& args]
  (log/info "Starting oauth core -main")
  (oauth/populate-tokens!))

;; Token-map new value: {
;;                       "access_token" : "ya29.Glv1A7u27r7FdPdD8DvLNPGjdcUg9Q_-WdqEwHw9qdhIV_buHKNyNjGTb5gBIDZD_7dBKA_5AQj11cIbkMMf6gOLm5-sM_9sE6VaWq-90zeS4gaMmocU5BmQmkDz",
;;                       "expires_in" : 3600,
;;                       "refresh_token" : "1/0DfX-0Jr-1aSrr1HvrdrLVBGfPa6bF3lQvT6e9FkSFw",
;;                       "token_type" : "Bearer"
;;                       }
