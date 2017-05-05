(ns thehex.notube.core
  (:require
   [clojure.edn :as edn]
   [taoensso.timbre :as log]
   [thehex.youtube-api3.core :as yt]
   [thehex.notube.util :as util]
   [thehex.oauth.lib :as oauth])
  (:gen-class))

(println "loading ..... thehex.notube.core yo!")
;; (defn -main
;;   "FIXME"
;;   [& args]
;;   (println (str "Main doesn't do anything... hello: " (+ 2 2))))

(declare parse-comments
         handle-comment
         handle-video
         is-spam?
         report-comment)

(def blacklist (edn/read-string
                (slurp (clojure.java.io/resource "blacklist.edn"))))

;; (def videos (edn/read-string
;;                 (slurp (clojure.java.io/resource "sample-data/videos.edn"))))

(defn parse-videos
  "Loop through videos, find all comment threads, scan each,
  store bad ones on a file... "
  [videos]
  ;; for now, parse only one video. we'll later call a doseq etc to handle all of them
  (map handle-video videos))

(defn handle-video
  [video]
  ;; todo... remove this next assertion line after debugging
  (assert (= "PewDiePie" (get video "channelTitle")) "Unexpected channel name!??")
  (log/infod "Handling a video for channel: %s" (get video "channelTitle"))
  (log/debugf "Handling video with title: %s\n" (get video "title"))
  (let [videoId (get (get video "id") "videoId")]
    ;; TODO: go through all pages of comments...
    (parse-comments (yt/get-video-commentThreads videoId))))

(defn parse-comments
  "loop through all comments from one page"
  [comments]
  (map handle-comment comments))

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
  (spit (util/with-abs-path (if is-spam "spam-queue" "white-queue"))
        (str (comment->id-text-pair comment) "\n")
        :append true))

;; (defn get-comment-sentiment
;;   "TODO: We can use the sentiment/perception api to get how negative a comment is."
;;   []
;;   "")

(defn handle-comment
  "Store comment into spam-queue or whitelist queue."
  [comment]
  (let [text (get-in (first sc) ["snippet" "topLevelComment" "snippet" "textOriginal"])
        author (get-in comment ["snippet" "topLevelComment" "snippet" "authorDisplayName"])]
    (if (some nil? [text author])
      (log/debugf "This parsed comment: %s \n\n doesnt have text and/or author!" comment)
      (do
        (log/debugf "Handling comment, commented by user: %s" author)
        (log/debugf "Comment text is: %s" text)
        (store-comment comment (is-spam? text author))))))

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
   (some #(str-in? text %) (:spam blacklist))))

(defn handle-all-videos
  "call yt api for videos, then continually call parse-videos on the set of results,
  by using the nextPageToken if its available on response body
  'UC-lHJZR3Gqxm24_Vd_AJ5Yw'"
  [channel-id]
  ;; TODO: go to all pages if there are more videos...
  (parse-videos (yt/search-videos channel-id)))

;; (defn handle-all-comments
;;   "call yt api for videos, and use in handle-video to contunually query for all comments,
;;   and be able to report from all of them. by using nextPageToken if its available
;;   in response body."
;;   []
;;   "")
