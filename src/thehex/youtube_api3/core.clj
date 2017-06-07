(ns thehex.youtube-api3.core
  (:require [thehex.youtube-api3.config :as config]
            [thehex.oauth.lib :as oauth]
            [clj-http.client :as http]
            [clojure.edn :as edn]
            [clojure.data.json :as json]
            [thehex.notube.config :as notube-config]
            [taoensso.timbre :as log])
  (:use [slingshot.slingshot :only [try+ throw+]]))

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
  Use like: (get-video-commentThreads  \"d2dNb0wdJF0\")
  "
  ([video-id page-token]
   (try+
    (let [url (str config/api-base "commentThreads?part=snippet&videoId="
                   video-id "&order=time&key=" api-key
                   (when page-token
                     (str "&pageToken=" page-token)))
          body (-> (http/get url {:as :json}) :body)]
      (log/tracef "Got comments from search: %s" body)
      (let [as-json (json/read-str body)]
        [(get as-json "items") (get as-json "nextPageToken")]))
    (catch [:status 401] e (log/error e))))
  ([video-id]
   (get-video-commentThreads video-id nil)))

(defn get-channel-activity
  "Original Url: https://www.googleapis.com/youtube/v3/activities?part=snippet&channelId=UC-lHJZR3Gqxm24_Vd_AJ5Yw&key=

  Use like: `(get-channel-activity api-key 'UC-lHJZR3Gqxm24_Vd_AJ5Yw')`


  my channel= UC4-vzjcBolmvYWYP6ldbLbA
  another channel id = UC4u8goEsLgpPvDX2mKD70nQ"
  ([channel-id page-token]
   (let [url (str config/api-base "activities?part=snippet&channelId=" channel-id
                  "&key=" api-key (when page-token (str "&pageToken=" page-token)))
         body (-> (http/get url {:as :json}) :body)]
     (log/debug (str "Got activites: " body))
     body))
  ([channel-id]
   (get-channel-activity channel-id nil)))

(defn search-videos
  "Original Url: https://www.googleapis.com/youtube/v3/search?part=snippet&channelId=UC-lHJZR3Gqxm24_Vd_AJ5Yw&key=

  Use like: `(search-videos 'UC-lHJZR3Gqxm24_Vd_AJ5Yw')`
  ^ pewds channel

  my channel= UC4-vzjcBolmvYWYP6ldbLbA
  another channel id = UC4u8goEsLgpPvDX2mKD70nQ
  "
  ([channel-id page-token]
   (let [url (str config/api-base "search?part=snippet&channelId=" channel-id
                  "&key=" api-key "&order=date"
                  (when page-token
                    (str "&pageToken=" page-token)))
         body (-> (http/get url {:as :json}) :body)]
     (log/debug (str "Got videos from search: " body))
     (let [as-json (json/read-str body)
           page-info (get as-json "pageInfo")]
       (log/debugf "Total Results: %s, resultsPerPage: %s" (get page-info "totalResults") (get page-info "resultsPerPage"))
       [(get as-json "items") (get as-json "nextPageToken")])))
  ([channel-id]
   (search-videos channel-id nil)))

(defn report-comment-as-spam
  "Reports a comment as spam, provided a comment id.
   This could fail for several reasons, including:
  - Comments not based on Google+ cannot be marked as spam.
  - insufficient permissions. The request might not be properly authorized.
  - commentNotFound"
  [comment-id]
  (try+
   (let [url (str config/api-base "comments/markAsSpam?id=" comment-id)
         res (http/post url
                        {:headers {:Authorization
                                   (str "Bearer " (:access-token tokens))}
                         :as :json})]
     (log/info "Sucessfully reported comment as spam, with result: ")
     (log/debug (str res)) ;; (:status res) == 204 no content
     true)
   (catch [:status 401] e (log/error e) false)
   (catch [:status 400] e (log/error e) false)
   (catch [:status 403] e (log/error e) false)
   (catch [:status 404] e (log/error e) false)))
