(ns thehex.notube.config
  (:require
   [taoensso.timbre :as timbre]
   ;;       :refer (log  trace  debug  info  warn  error  fatal  report
   ;;               logf tracef debugf infof warnf errorf fatalf reportf
   ;;               spy get-env log-env
   [thehex.notube.util :as util]
   [taoensso.timbre.appenders.core :as appenders]))

(def ^:const log-level
  (if util/prod?
    (keyword (util/read-config-key :log-level))
    :trace))

(def timbre-config
  {:level log-level  ; e/o #{:trace :debug :info :warn :error :fatal :report}
   ;; Control log filtering by namespaces/patterns. Useful for turning off
   ;; logging in noisy libraries, etc.:
   ;;    :ns-whitelist  [] #_["my-app.foo-ns"]
   :ns-blacklist  [] #_["taoensso.*"]
   :middleware [] ; (fns [data]) -> ?data, applied left->right
   ;; Clj only:
   ;;    :timestamp-opts default-timestamp-opts ; {:pattern _ :locale _ :timezone _}
   ;;    :output-fn default-output-fn ; (fn [data]) -> string
   :appenders {:spit (appenders/spit-appender {:fname (util/with-abs-path "notube.log")})}})

(timbre/merge-config! timbre-config)
