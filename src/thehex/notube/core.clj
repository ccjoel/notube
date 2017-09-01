(ns thehex.notube.core
  (:require
   [clojure.edn :as edn]
   [thehex.youtube-api3.core :as yt]
   [thehex.notube.util :as util]
   [thehex.oauth.lib :as oauth])
  (:gen-class))

(declare parse-comments
         handle-comment
         handle-video
         handle-all-comments
         store-comment
         is-spam?)

(def blacklist (edn/read-string
                (slurp (clojure.java.io/resource "blacklist.edn"))))

(defn parse-videos
  "Loop through videos, find all comment threads, scan each,
  store bad ones on a file... "
  [videos]
  (doseq [video videos] (handle-video video)))

(defn handle-video
  [video]
  (let [video-id (get (get video "id") "videoId")]
    (future (handle-all-comments video-id))))

(defn parse-comments
  "loop through all comments from one page"
  [comments]
  (doseq [comment comments] (handle-comment comment)))

(defn handle-comment
  "Store comment into spam-queue or whitelist queue."
  [comment]
  (let [text (get-in comment ["snippet" "topLevelComment" "snippet" "textOriginal"])
        author (get-in comment ["snippet" "topLevelComment" "snippet" "authorDisplayName"])]
    (if (some nil? [text author])
      (println "This parsed comment doesnt have text and/or author!" comment)
      (do
        ;; (println "Handling comment, commented by user: " author)
        ;; (println "Comment text is: " text)
        (store-comment comment (is-spam? text author))))))

(defn comment->id-text-pair
  "returns a simple object formatted to better spit to file"
  [comment]
  {:id (get comment "id")
   :text (get-in comment ["snippet" "topLevelComment" "snippet" "textOriginal"])})

(defn store-comment
  "Since the algorithms and methods could be wrong. We'll store comments for processing and
  manually inspect them to verify that all should be reported. If they are, some other fn
  will take care of the actual reporting.
  We only need to store the comment-id and the textOriginal.
  We'll either store to spam queue, ot to a whitelist queue to check for false
  negatives + find new spam sources."
  [comment is-spam]
  (println "storing comment")
  (spit (util/with-abs-path (if is-spam "spam-queue" "white-queue"))
        (str (comment->id-text-pair comment) "\n")
        :append true))

;; (defn get-comment-sentiment
;;   "TODO: We can use the sentiment/perception api to get how negative a comment is."
;;   []
;;   "")

(defmulti str-in?
  (fn [in store]
    (type store)))

(defmethod str-in? clojure.lang.PersistentVector [in store]
  (every? #(.contains (clojure.string/lower-case in) (clojure.string/lower-case %)) store))

(defmethod str-in? java.lang.String [in store]
  (.contains (clojure.string/lower-case in) (clojure.string/lower-case store)))

(defn is-spam?
  "Receives a comment text, and an author name, and return true if spam,
  dictated by the terms under blacklist.edn"
  [text author]
  (or
   (some #(= (clojure.string/lower-case author) (clojure.string/lower-case %)) (:users blacklist))
   (some #(str-in? text %) (:spam blacklist))
   (some #(str-in? text %) (:bullying blacklist))
   (some #(str-in? text %) (:hate-speech blacklist))))

(defn handle-all-channel-videos
  "Call youtube api for videos, then continually call parse-videos on the set of results,
  by using the nextPageToken if its available on response body
  'UC-lHJZR3Gqxm24_Vd_AJ5Yw'"
  ([channel-id page-token]
   (let [[videos next-page-token] (yt/search-channel-videos channel-id page-token)]
     (future (parse-videos videos))
     (when next-page-token
       (handle-all-channel-videos channel-id next-page-token))))
  ([channel-id]
   (handle-all-channel-videos channel-id nil)))

(defn handle-all-comments
  "Call yt api for videos, and use in handle-video to continually query for all comments,
  and be able to report from all of them. by using nextPageToken if its available
  in response body."
  ([video-id page-token]
   (let [[comments next-page-token] (yt/get-video-commentThreads video-id page-token)]
     (future (parse-comments comments))
     (when next-page-token
       (handle-all-comments video-id next-page-token))))
  ([video-id]
   (handle-all-comments video-id nil)))

(defn report-spam-line
  "Report only one spam line, which is a string with unparsed edn format. id is
   of the form: {:id \"some-id\" :text \"text in comment\"}"
  [edn-str-line]
  (let [parsed (edn/read-string edn-str-line)]
    (yt/report-comment-as-spam (:id parsed))
    ;; then return text to print.. for now...
    ;; TODO: remove the line parsed to not print anything. not needed, really
    (:text parsed)))

(defn report-spam-queue
  "REPORTS ALL THE SPAM QUEUE WITH YOUTUBE API. Be Careful here."
  []
  ;; TODO: maybe clear or do something to spam queue.. what happens if we try to report twice?
  (try
    (let [file (util/with-abs-path "spam-queue")]
      (println "Reporting ALL SPAM msgs from spam queue!")
      (util/process-file-by-lines file report-spam-line identity))
    (catch java.io.FileNotFoundException e
      (binding [*out* *err*]
        (println "spam-queue file not found. Please run notube -n <channel-id> first."))
      (System/exit 2))))

;; (def videos (edn/read-string
;;              (slurp (clojure.java.io/resource "sample-data/videos.edn"))))
