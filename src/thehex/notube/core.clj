(ns thehex.notube.core
  (:require
   [thehex.youtube-api3.core :as yt]
   [taoensso.timbre :as log]
   [clojure.edn :as edn]
   [thehex.oauth.lib :as oauth])
  (:gen-class))

(defn -main
  "FIXME"
  [& args]
  (println (str "Main doesnt do anything... hello: " (+ 2 2))))

(println "loading ..... thehex.notube.core yo!")

(declare parse-comments
         handle-comment
         handle-video
         is-spam?
         report-comment)

(def blacklist (edn/read-string
                (slurp (clojure.java.io/resource "blacklist.edn"))))

(def videos (edn/read-string
                (slurp (clojure.java.io/resource "sample-data/videos.edn"))))

(defn parse-videos
  "Loop through videos, find all comment threads, scan each,
  store bad ones on a file...
  todo: add pagination to video results?
  "
  [videos]
  ;; for now, parse only one video. we'll later call a doseq etc to handle everything
  (handle-video (first videos)))

(defn handle-video
  [video]
  ;; todo... remove this next assertion line after debugging
  (assert (= "PewDiePie" (get video "channelTitle")) "Unexpected channel name!??")
  (log/infod "Handling a video for channel: %s" (get video "channelTitle"))
  (log/debugf "Handling video with title: %s\n" (get video "title"))
  ;; get videoId
  (let [videoId (get (get video "id") "videoId")]
    ;; get commentThreads for...
    (parse-comments (yt/get-video-commentThreads videoId))))

(defn parse-comments
  "todo: add pagination to video results?"
  [comments]
  (map handle-comment comments))

(defn handle-comment
  " todo... report using youtube api "
  [comment]
  (let [text (get-in (first sc) ["snippet" "topLevelComment" "snippet" "textOriginal"])
        author (get-in comment ["snippet" "topLevelComment" "snippet" "authorDisplayName"])]
    (if (some nil? [text author])
      (log/debugf "This parsed comment: %s \n\n doesnt have text and/or author!" comment)
      (do
        (log/debugf "Handling comment, commented by user: %s" author)
        (log/debugf "Comment text is: %s" text)
        (when (is-spam? text author)
          (log/info "Last comment is spam! Report!"))))))

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
