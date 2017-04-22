(ns thehex.youtube-api3.core
  (:require [thehex.youtube-api3.config :as config]
            [thehex.oauth.lib :as oauth]
            [clj-http.client :as http]
            [clojure.edn :as edn]
            [clojure.data.json :as json]
            [thehex.notube.config :as notube-config]
            [taoensso.timbre :as log])
  (:use [slingshot.slingshot :only [try+ throw+]]))

;; TODO: use YOUTUBE_API_KEY environmental variable

;; - Specific Comment by Id
;; https://www.googleapis.com/youtube/v3/comments?id=z124jjhzrlfitdvcw23xfzyrdya4ij0kj&part=snippet&key=

;; - Video info (statistics.. change part to get different info)
;; https://www.googleapis.com/youtube/v3/videos?part=statistics&id=UTXCu1VQDRw&key=

;; - Search all videos of pewdiepie's channel, ordered by date, newest first
;; https://www.googleapis.com/youtube/v3/search?part=snippet&channelId=UC-lHJZR3Gqxm24_Vd_AJ5Yw&order=date&key=

;; With OAUTH and logged in w/ youtube user:
;; POST https://www.googleapis.com/youtube/v3/commenThreads?part=snippet&key=
;; post raw json body:
;; {
;;   "snippet": {
;;     "channelId": "UC-lHJZR3Gqxm24_Vd_AJ5Yw",
;;     "topLevelComment": {
;;       "snippet": {
;;         "textOriginal": "comment3"
;;       }
;;     },
;;     "videoId": "TvxkKs2J0Vw"
;;   }
;; }

(def tokens (oauth/read-persisted-tokens))

;; TODO: don't store api-key in resources. Allow default location for these configs
;;       and allow user to specify a different folder for configs.
;;       then read all json/edn configuration from that folder
(def api-key (->
              (edn/read-string (slurp (clojure.java.io/resource "config.edn")))
              :api-key))

(defn get-user-channels
  " Example...
  The API supports two ways to specify an access token:
  1. curl -H \"Authorization: Bearer ACCESS_TOKEN\" https://www.googleapis.com/youtube/v3/channels?part=id&mine=true
  2. curl https://www.googleapis.com/youtube/v3/channels?part=id&mine=true&access_token=ACCESS_TOKEN
  TODO: try catch 401. if 401, we nee to refresh the access token
  TODO: this call is not part of the oauth validation file, is an actual api call. move
  Use like: (get-user-channels) "
  []
  (try+
   (let [url (str config/api-base "channels?part=id&mine=true")
         body (-> (http/get url
                            {:headers {:Authorization
                                       (str "Bearer " (:access-token tokens))}
                             :as :json})
                  :body ;; body is json
                  ;; TODO: parse response json to actually get info
                  ;; will receive a 401 HTTP unauthorize if the access token expired
                  ;;:user ... old example on getting data...
                  )
         as-json (json/read-str body)
         result-count (get (get as-json "pageInfo") "totalResults")]
     (log/debug (str "Got body in get-user-channels: " body))

     (if (> result-count 0)
       (get as-json "items")
       nil))
   (catch [:status 401] e (log/error e))))

(defn get-video-commentThreads
  " https://www.googleapis.com/youtube/v3/commentThreads?part=snippet&videoId=UTXCu1VQDRw&key=
  Use like: (get-video-commentThreads 84389473498348)

   TODO: use fn arity or optional param? to use nextPageToken, if available, in
         order to retrieve the rest pages of results
  "
  [video-id]
  (try+
   (let [url (str config/api-base "commentThreads?part=snippet&videoId="
                  video-id "&order=time&key=" api-key)
         body (-> (http/get url {:as :json}) :body)]
     (log/debug (str "Got videos from search: " body))
     (let [as-json (json/read-str body)
           items (get as-json "items")]
       (log/trace "Got comments for a video..")
       items))
   (catch [:status 401] e (log/error e))))

(defn get-channel-activity
  "Original Url: https://www.googleapis.com/youtube/v3/activities?part=snippet&channelId=UC-lHJZR3Gqxm24_Vd_AJ5Yw&key=

  Use like: `(get-channel-activity api-key 'UC-lHJZR3Gqxm24_Vd_AJ5Yw')`


  my channel= UC4-vzjcBolmvYWYP6ldbLbA
  another channel id = UC4u8goEsLgpPvDX2mKD70nQ
  TODO: add an optional pagination param, by using fn arity, to retrieve that specific page
  the id on the json payload is called 'nextPageToken'"
  [channel-id]
  (let [url (str config/api-base "activities?part=snippet&channelId=" channel-id "&key=" api-key)
        body (-> (http/get url {:as :json}) :body)]
    (log/debug (str "Got activites: " body))
    body))

(defn search-videos
  "Original Url: https://www.googleapis.com/youtube/v3/search?part=snippet&channelId=UC-lHJZR3Gqxm24_Vd_AJ5Yw&key=

  Use like: `(search-videos api-key 'UC-lHJZR3Gqxm24_Vd_AJ5Yw')`
  ^ pewds channel

  my channel= UC4-vzjcBolmvYWYP6ldbLbA
  another channel id = UC4u8goEsLgpPvDX2mKD70nQ

  TODO: add an optional pagination param, by using fn arity
  "
  [channel-id]
  (let [url (str config/api-base "search?part=snippet&channelId=" channel-id
                 "&key=" api-key "&order=date")
        body (-> (http/get url {:as :json}) :body)]
    (log/debug (str "Got videos from search: " body))
    (let [as-json (json/read-str body)]
      ;; TODO: go through the items, parse and getting info we want
      (get as-json "items"))))

(defn report-comment-as-spam
  "TODO: "
  [commentId]
  "")


;; (get-video-commentThreads api-key "d2dNb0wdJF0")

;; (search-videos api-key "UC-lHJZR3Gqxm24_Vd_AJ5Yw")
