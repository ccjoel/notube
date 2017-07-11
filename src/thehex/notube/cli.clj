(ns thehex.notube.cli
  (:require [clojure.tools.cli :refer [parse-opts]]
            [thehex.notube.util :as util]
            [thehex.notube.core :as notube]
            [thehex.oauth.lib :as oauth]
            [taoensso.timbre :as log])
  (:gen-class))

(def cli-options
  [;; login and populate tokens, or refresh access token
   ["-t" "--tokens ACTION" "Populate or refresh tokens"
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

(defn -main [& args]
  (let [clargs (parse-opts args cli-options)
        opts (:options clargs)]
    (cond
      (:errors clargs) (doseq [e (:errors clargs)] (println e))
      (= (:tokenaction opts) "p") (oauth/populate-tokens!)
      (= (:tokenaction opts) "r") (log/info "should refresh access token")
      (:notube opts) (notube/handle-all-channel-videos)
      (:report opts) (log/info "should report spam queue with youtube api")
      :else (log/infof "Received these args: %s.\n Summary:\n %s" args (:summary clargs)))))


;; (def clargs '("-t" "r"))
;; Execute the command line:
;; my-program -vvvp8080 foo --help --invalid-opt
;; to produce the map:
;; {:options   {:port 8080
;;              :verbosity 3
;;              :help true}
;;  :arguments ["foo"]
;;  :summary   "  -p, --port PORT  80  Port number
;;                -v                   Verbosity level
;;                -h, --help"
;;  :errors    ["Unknown option: \"--invalid-opt\""]}
