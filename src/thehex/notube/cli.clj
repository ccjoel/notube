(ns thehex.notube.cli
  (:require [clojure.tools.cli :refer [parse-opts]]
            [thehex.notube.util :as util]
            [thehex.notube.core :as notube]
            [thehex.youtube-api3.core :as yt]
            [thehex.oauth.lib :as oauth]
            [taoensso.timbre :as log])
  (:gen-class))

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
   ["-n" "--notube CHANNELID" "Scan and populate spam queue"
    :id :channel-id]
   ["-r" "--report" "Go through spam queue and report as spam"]
   ["-s" "--search-channel CHANNEL-OR-USER-NAME" "Search users by name, receive channel id"
    :id :username]
   ;; A boolean option defaulting to nil
   ["-h" "--help"]])

;; TODO: doesnt need to be the main app, could be a fn that core.clj calls
(defn -main [& args]
  (let [clargs (parse-opts args cli-options)
        opts (:options clargs)]
    (cond
      ;; (:errors clargs) (doseq [e (:errors clargs)] (println e))
      (= (:tokenaction opts) "p") (oauth/populate-tokens!)
      (= (:tokenaction opts) "r") (oauth/refresh-tokens!)
      (:channel-id opts) (notube/handle-all-channel-videos (:channel-id opts)) ;; need to pass channel id
      (:username opts) (yt/search-users (:username opts))
      (:report opts) (notube/report-spam-queue)
      :else (do (log/infof "Notube v0.1.1\nUsage:\n%s\n" (:summary clargs))
                (System/exit 0)))))
