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
    (keyword (util/read-config :log-level))
    :trace))

(def timbre-config
  {:level log-level  ; e/o #{:trace :debug :info :warn :error :fatal :report}
   :ns-blacklist  [] #_["taoensso.*"]
   :middleware [] ; (fns [data]) -> ?data, applied left->right
   ;; timbre/default-output-fn
   :appenders {:spit (appenders/spit-appender {:fname (util/with-abs-path "notube.log")})
               :println {:output-fn (fn [data]
                                      (if (:?msg-fmt data)
                                        (apply (partial printf (:?msg-fmt data)) (:vargs data))
                                        (apply str (:vargs data))))}}})

(timbre/merge-config! timbre-config)
