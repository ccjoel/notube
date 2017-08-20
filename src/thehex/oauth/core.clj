;; (ns thehex.oauth.core
;;   (:require [thehex.oauth.lib :as oauth]
;;             [thehex.notube.config :as config]
;;             [taoensso.timbre :as log]))

;; (defn -main
;;   ""
;;   [& args]
;;   (log/info "Starting oauth core -main: populate tokens!")
;;   (oauth/populate-tokens!))

;; (defn refresh
;;   ""
;;   [& args]
;;   (log/info "refresing token")
;;   (oauth/refresh (:refresh-token (oauth/read-persisted-tokens))))
