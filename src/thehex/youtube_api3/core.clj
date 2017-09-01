(ns thehex.youtube-api3.core
  (:require [thehex.youtube-api3.config :as config]
            [thehex.oauth.lib :as oauth]
            [org.httpkit.client :as http2]
            [thehex.notube.util :as util]
            [clojure.data.json :as json]))

(defn api-key
  []
  (let [apikey (util/read-config :api-key)]
    (or (and (not (empty? apikey)) apikey)
     (System/getenv "YOUTUBE_API_KEY")
     (do
       (println "No api key on config file found. Please update on config.edn")
       (System/exit 3)))))

(defn api-call
  ""
  [url]
  (try
   (let [{:keys [status headers error body]} @(http2/get url)]
     (println "Status: \n Headers: \n error: \n" status "\n" headers "\n" error)
     body)
   (catch Exception e
     (binding [*out* *err*]
       (println e))
     (System/exit 4))))

(defn get-video-commentThreads
  "https://www.googleapis.com/youtube/v3/commentThreads?part=snippet&videoId=UTXCu1VQDRw&key=
   Use like: (get-video-commentThreads  \"d2dNb0wdJF0\")"
  ([video-id page-token]
   (let [url (str config/api-base "commentThreads?part=snippet&videoId="
                  video-id "&order=time&key=" (api-key)
                  (when page-token
                    (str "&pageToken=" page-token)))
         body (api-call url)]
     (let [as-json (json/read-str body)]
       [(get as-json "items") (get as-json "nextPageToken")])))
  ([video-id]
   (get-video-commentThreads video-id nil)))

(defn get-channel-activity
  "Original Url: https://www.googleapis.com/youtube/v3/activities?part=snippet&channelId=UC-lHJZR3Gqxm24_Vd_AJ5Yw&key=
  Use like: `(get-channel-activity api-key 'UC-lHJZR3Gqxm24_Vd_AJ5Yw')`
  my channel= UC4-vzjcBolmvYWYP6ldbLbA
  another channel id = UC4u8goEsLgpPvDX2mKD70nQ"
  ([channel-id page-token]
   (let [url (str config/api-base "activities?part=snippet&channelId=" channel-id
                  "&key=" (api-key)
                  (when page-token (str "&pageToken=" page-token)))
         body (api-call url)]
     (println (str "Got activites: " body))
     body))
  ([channel-id]
   (get-channel-activity channel-id nil)))

(defn search-channel-videos
  "Original Url: https://www.googleapis.com/youtube/v3/search?part=snippet&channelId=UC-lHJZR3Gqxm24_Vd_AJ5Yw&key=

  Use like: `(search-channel-videos 'UC-lHJZR3Gqxm24_Vd_AJ5Yw')`
  ^ pewds channel

  my channel= UC4-vzjcBolmvYWYP6ldbLbA
  another channel id = UC4u8goEsLgpPvDX2mKD70nQ
  "
  ([channel-id page-token]
   (let [url (str config/api-base "search?part=snippet&channelId=" channel-id
                  "&key=" (api-key) "&order=date"
                  (when page-token
                    (str "&pageToken=" page-token)))
         body (api-call url)]
     (println (str "Got videos from search: " body))
     (let [as-json (json/read-str body)
           page-info (get as-json "pageInfo")]
       (println "Total Results: , resultsPerPage: " (get page-info "totalResults") (get page-info "resultsPerPage"))
       [(get as-json "items") (get as-json "nextPageToken")])))
  ([channel-id]
   (search-channel-videos channel-id nil)))

(defn search-users
  "Original Url: https://www.googleapis.com/youtube/v3/search?part=snippet&q=someusername&key=

  Use like: `(search-users \"pewdiepie\")`

  my channel= UC4-vzjcBolmvYWYP6ldbLbA
  another channel id = UC4u8goEsLgpPvDX2mKD70nQ
  "
  [query]
  (let [url (str config/api-base "search?part=snippet&q=" query
                 "&type=channel" "&maxResults=50" "&key=" (api-key))
        body (api-call url)]
    (let [as-json (json/read-str body)
          page-info (get as-json "pageInfo")]
      (println "Total Results: , resultsPerPage: "
                  (get page-info "totalResults") (get page-info "resultsPerPage"))
      (let [items (get as-json "items")]
        (println (apply str
                         (map
                          (fn [item]
                            (str (get (get item "snippet") "title") ": " (get (get item "id") "channelId") "\n"))
                          (filter (fn [item]
                                    (let [id (get item "id")]
                                      (= (get id "kind") "youtube#channel")))
                                  items))))))))

(defn report-comment-as-spam
  "Reports a comment as spam, provided a comment id.
   This could fail for several reasons, including:
  - Comments not based on Google+ cannot be marked as spam.
  - insufficient permissions. The request might not be properly authorized.
  - commentNotFound"
  [comment-id]
  (try
   (let [url (str config/api-base "comments/markAsSpam?id=" comment-id)
         res @(http2/post url
                        {:headers {"Authorization"
                                   (str "Bearer " (:access-token (oauth/read-persisted-tokens)))}})]
     (println "Sucessfully reported comment as spam.")
     true)
   (catch Exception e
     (println "got 401 or so on report spam" e)
     false)))
