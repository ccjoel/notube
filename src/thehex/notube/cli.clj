(ns thehex.notube.cli
  (:require [clojure.tools.cli :refer [parse-opts]]
            [thehex.notube.util :as util]
            [thehex.notube.core :as notube]
            [thehex.oauth.lib :as oauth]
            [taoensso.timbre :as log])
  (:gen-class))

;; TODO: maybe -p and -r for populate/refresh. instead of -t then p ir r...
(def cli-options
  [;; login and populate tokens, or refresh access token
   ["-t" "--tokens ACTION" "Populate or refresh tokens. Action can be either p o r"
    :id :tokenaction
    :validate [#(or (= "r" %) (= "p" %))]]
   ;; ["-v" nil "Verbosity level"
   ;;  :id :verbosity
   ;;  :default 0
   ;;  :assoc-fn (fn [m k _] (update-in m [k] inc))]
   ;; notube!
   ["-n" "--notube" "Scan and populate spam queue"]
   ["-r" "--report" "Go through spam queue and report as spam"]
   ;; A boolean option defaulting to nil
   ["-h" "--help"]])

;; TODO: later... possibly doesnt need to be the main app, specially since core.clj could call parse-opts args...
(defn -main [& args]
  (let [clargs (parse-opts args cli-options)
        opts (:options clargs)]
    (cond
      (:errors clargs) (doseq [e (:errors clargs)] (println e))
      (= (:tokenaction opts) "p") (oauth/populate-tokens!)
      (= (:tokenaction opts) "r") (oauth/refresh-tokens!)
      (:notube opts) (notube/handle-all-channel-videos)
      (:report opts) (log/info "should report spam queue with youtube api")
      :else (log/infof "Received these args: %s.\n Summary:\n %s" args (:summary clargs)))))
