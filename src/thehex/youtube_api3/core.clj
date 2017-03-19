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
  Use like: (get-video-commentThreads 84389473498348) "
  [video-id]
  (try+
   (let [url (str config/api-base "commentThreads?part=snippet&videoId="
                  video-id "&order=time&key=" api-key)
         body (-> (http/get url {:as :json}) :body)]
     (log/debug (str "Got videos from search: " body))
     (let [as-json (json/read-str body)
           items (get as-json "items")]
       ;; TODO: go through the items, parse and getting info we want
       (log/trace "Got comments for a video:")
       items))
   (catch [:status 401] e (log/error e))))

(defn get-channel-activity
  "Original Url: https://www.googleapis.com/youtube/v3/activities?part=snippet&channelId=UC-lHJZR3Gqxm24_Vd_AJ5Yw&key=

  Use like: `(get-channel-activity api-key 'UC-lHJZR3Gqxm24_Vd_AJ5Yw')`


  my channel= UC4-vzjcBolmvYWYP6ldbLbA
  another channel id = UC4u8goEsLgpPvDX2mKD70nQ "
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
  another channel id = UC4u8goEsLgpPvDX2mKD70nQ "
  [channel-id]
  (let [url (str config/api-base "search?part=snippet&channelId=" channel-id
                 "&key=" api-key "&order=date")
        body (-> (http/get url {:as :json}) :body)]
    (log/debug (str "Got videos from search: " body))
    (let [as-json (json/read-str body)]
      ;; TODO: go through the items, parse and getting info we want
      (get as-json "items"))))

(defn parse-videos
  "Loop through videos, find all comment threads, scan each,
  store bad ones on a file..."
  []
  "")

;; (get-video-commentThreads api-key "d2dNb0wdJF0")

;; (search-videos api-key "UC-lHJZR3Gqxm24_Vd_AJ5Yw")

;; videos for a channel:
;; [{"kind" "youtube#searchResult",
;;  "etag" "\"uQc-MPTsstrHkQcRXL3IWLmeNsM/xL2Kk-wHmX7jWjGq4UC1iRNQW00\"",
;;  "id" {"kind" "youtube#video",
;;  "videoId" "d2dNb0wdJF0"},
;;  "snippet" {"publishedAt" "2017-02-18T18:01:07.000Z",
;;  "channelId" "UC-lHJZR3Gqxm24_Vd_AJ5Yw",
;;  "title" "YOU LAUGH? YOU LOSE! CHALLENGE",
;;  "description" "ヅWatch PART 1 https://www.youtube.com/watch?v=TS3dxL0E1kcヅ [Ad:] Check out my current Giveaway w/ G2A: ...",
;;  "thumbnails" {"default" {"url" "https://i.ytimg.com/vi/d2dNb0wdJF0/default.jpg",
;;  "width" 120,
;;  "height" 90},
;;  "medium" {"url" "https://i.ytimg.com/vi/d2dNb0wdJF0/mqdefault.jpg",
;;  "width" 320,
;;  "height" 180},
;;  "high" {"url" "https://i.ytimg.com/vi/d2dNb0wdJF0/hqdefault.jpg",
;;  "width" 480,
;;  "height" 360}},
;;  "channelTitle" "PewDiePie",
;;  "liveBroadcastContent" "none"}} {"kind" "youtube#searchResult",
;;  "etag" "\"uQc-MPTsstrHkQcRXL3IWLmeNsM/utQCEFCHDH4xu2oML31EBjPqekI\"",
;;  "id" {"kind" "youtube#video",
;;  "videoId" "dSOGGGgXh7Q"},
;;  "snippet" {"publishedAt" "2017-02-17T17:18:37.000Z",
;;  "channelId" "UC-lHJZR3Gqxm24_Vd_AJ5Yw",
;;  "title" "UNREAL MIND TRICK! (99% CANT DO THIS)",
;;  "description" "mind = blown ヅ.",
;;  "thumbnails" {"default" {"url" "https://i.ytimg.com/vi/dSOGGGgXh7Q/default.jpg",
;;  "width" 120,
;;  "height" 90},
;;  "medium" {"url" "https://i.ytimg.com/vi/dSOGGGgXh7Q/mqdefault.jpg",
;;  "width" 320,
;;  "height" 180},
;;  "high" {"url" "https://i.ytimg.com/vi/dSOGGGgXh7Q/hqdefault.jpg",
;;  "width" 480,
;;  "height" 360}},
;;  "channelTitle" "PewDiePie",
;;  "liveBroadcastContent" "none"}} {"kind" "youtube#searchResult",
;;  "etag" "\"uQc-MPTsstrHkQcRXL3IWLmeNsM/VX9HxXX5h0TslJOi2zy_Bw42jzM\"",
;;  "id" {"kind" "youtube#video",
;;  "videoId" "lwk1DogcPmU"},
;;  "snippet" {"publishedAt" "2017-02-16T17:07:05.000Z",
;;  "channelId" "UC-lHJZR3Gqxm24_Vd_AJ5Yw",
;;  "title" "My Response",
;;  "description" "ヅ My statement about hate groups supporting me: http://pewdie.tumblr.com/post/157160889655/just-to-clear-some-things-up.",
;;  "thumbnails" {"default" {"url" "https://i.ytimg.com/vi/lwk1DogcPmU/default.jpg",
;;  "width" 120,
;;  "height" 90},
;;  "medium" {"url" "https://i.ytimg.com/vi/lwk1DogcPmU/mqdefault.jpg",
;;  "width" 320,
;;  "height" 180},
;;  "high" {"url" "https://i.ytimg.com/vi/lwk1DogcPmU/hqdefault.jpg",
;;  "width" 480,
;;  "height" 360}},
;;  "channelTitle" "PewDiePie",
;;  "liveBroadcastContent" "none"}} {"kind" "youtube#searchResult",
;;  "etag" "\"uQc-MPTsstrHkQcRXL3IWLmeNsM/XVEo4--KVgc4qdee8aDJ-qqv9so\"",
;;  "id" {"kind" "youtube#video",
;;  "videoId" "_TSZe3mfGYg"},
;;  "snippet" {"publishedAt" "2017-02-14T16:57:05.000Z",
;;  "channelId" "UC-lHJZR3Gqxm24_Vd_AJ5Yw",
;;  "title" "VALENTINE'S SPECIAL!",
;;  "description" "ヅヅヅヅヅヅヅヅヅヅヅヅヅヅヅヅヅヅヅヅヅヅヅヅヅヅヅヅヅヅヅヅ [Ad:] Check out my current Giveaway w/ G2A: ...",
;;  "thumbnails" {"default" {"url" "https://i.ytimg.com/vi/_TSZe3mfGYg/default.jpg",
;;  "width" 120,
;;  "height" 90},
;;  "medium" {"url" "https://i.ytimg.com/vi/_TSZe3mfGYg/mqdefault.jpg",
;;  "width" 320,
;;  "height" 180},
;;  "high" {"url" "https://i.ytimg.com/vi/_TSZe3mfGYg/hqdefault.jpg",
;;  "width" 480,
;;  "height" 360}},
;;  "channelTitle" "PewDiePie",
;;  "liveBroadcastContent" "none"}} {"kind" "youtube#searchResult",
;;  "etag" "\"uQc-MPTsstrHkQcRXL3IWLmeNsM/WIDsZNZwinLXlzJI7JDKFiQzpcs\"",
;;  "id" {"kind" "youtube#video",
;;  "videoId" "afdbYnQsadA"},
;;  "snippet" {"publishedAt" "2017-02-13T16:33:39.000Z",
;;  "channelId" "UC-lHJZR3Gqxm24_Vd_AJ5Yw",
;;  "title" "WHO DO PEOPLE HATE?",
;;  "description" "ヅBG Kumbi thinks he can challenge me,
;;  sadヅ [Ad:] Check out my current Giveaway w/ G2A: https://gleam.io/Jytzw/pewdiepie-february-giveaway.",
;;  "thumbnails" {"default" {"url" "https://i.ytimg.com/vi/afdbYnQsadA/default.jpg",
;;  "width" 120,
;;  "height" 90},
;;  "medium" {"url" "https://i.ytimg.com/vi/afdbYnQsadA/mqdefault.jpg",
;;  "width" 320,
;;  "height" 180},
;;  "high" {"url" "https://i.ytimg.com/vi/afdbYnQsadA/hqdefault.jpg",
;;  "width" 480,
;;  "height" 360}},
;;  "channelTitle" "PewDiePie",
;;  "liveBroadcastContent" "none"}}]

;; ;; comments for a video:
;; [{"kind" "youtube#commentThread",
;;  "etag" "\"uQc-MPTsstrHkQcRXL3IWLmeNsM/2oC2KN5MSddxrkJ7nk1AmF5kbxU\"",
;;  "id" "z133i50x0srewbb1l223zlpx1oytiry2t",
;;  "snippet" {"videoId" "d2dNb0wdJF0",
;;  "topLevelComment" {"kind" "youtube#comment",
;;  "etag" "\"uQc-MPTsstrHkQcRXL3IWLmeNsM/Pye6dgF37Pg-M3q-ODjYWnO7drE\"",
;;  "id" "z133i50x0srewbb1l223zlpx1oytiry2t",
;;  "snippet" {"textOriginal" "The blonde hair with the pastel pink sweatshirt is such a good look.",
;;  "updatedAt" "2017-02-18T19:49:34.000Z",
;;  "authorProfileImageUrl" "https://yt3.ggpht.com/-YSlZnaBIuWM/AAAAAAAAAAI/AAAAAAAAAAA/rM6A67sLDwQ/s28-c-k-no-mo-rj-c0xffffff/photo.jpg",
;;  "authorDisplayName" "AlexisTheHipsta",
;;  "videoId" "d2dNb0wdJF0",
;;  "textDisplay" "The blonde hair with the pastel pink sweatshirt is such a good look.",
;;  "authorChannelUrl" "http://www.youtube.com/channel/UCvD5tOq_sE4-8vtQ0uz3hAw",
;;  "canRate" false,
;;  "authorChannelId" {"value" "UCvD5tOq_sE4-8vtQ0uz3hAw"},
;;  "likeCount" 0,
;;  "viewerRating" "none",
;;  "publishedAt" "2017-02-18T19:49:34.000Z"}},
;;  "canReply" false,
;;  "totalReplyCount" 0,
;;  "isPublic" true}} {"kind" "youtube#commentThread",
;;  "etag" "\"uQc-MPTsstrHkQcRXL3IWLmeNsM/Qb8XHO_j4KGnp1v3N1QKPcl6wig\"",
;;  "id" "z12shlsrdtz0gjrm023ytpdxypjxdhoni",
;;  "snippet" {"videoId" "d2dNb0wdJF0",
;;  "topLevelComment" {"kind" "youtube#comment",
;;  "etag" "\"uQc-MPTsstrHkQcRXL3IWLmeNsM/Jh6LP9oZnnxf8LKUU-c3zrrEPm8\"",
;;  "id" "z12shlsrdtz0gjrm023ytpdxypjxdhoni",
;;  "snippet" {"textOriginal" "Can i get subscribers for no reason ? :)",
;;  "updatedAt" "2017-02-18T19:49:32.000Z",
;;  "authorProfileImageUrl" "https://yt3.ggpht.com/-vSNB511vbx4/AAAAAAAAAAI/AAAAAAAAAAA/h2-c35MAj3E/s28-c-k-no-mo-rj-c0xffffff/photo.jpg",
;;  "authorDisplayName" "Mario Mihailov",
;;  "videoId" "d2dNb0wdJF0",
;;  "textDisplay" "Can i get subscribers for no reason ? :)",
;;  "authorChannelUrl" "http://www.youtube.com/channel/UC5-tPOqUiFO3YzXVt3nhrHQ",
;;  "canRate" false,
;;  "authorChannelId" {"value" "UC5-tPOqUiFO3YzXVt3nhrHQ"},
;;  "likeCount" 0,
;;  "viewerRating" "none",
;;  "publishedAt" "2017-02-18T19:49:32.000Z"}},
;;  "canReply" false,
;;  "totalReplyCount" 0,
;;  "isPublic" true}} {"kind" "youtube#commentThread",
;;  "etag" "\"uQc-MPTsstrHkQcRXL3IWLmeNsM/_v5JPAWQbGteqGgPhs1FS3pmVYc\"",
;;  "id" "z13jfjqqqyqcsf2qv04cilyhoymutxwaxho",
;;  "snippet" {"videoId" "d2dNb0wdJF0",
;;  "topLevelComment" {"kind" "youtube#comment",
;;  "etag" "\"uQc-MPTsstrHkQcRXL3IWLmeNsM/uM9-TsSL0tA_YcQG4L7wg593v14\"",
;;  "id" "z13jfjqqqyqcsf2qv04cilyhoymutxwaxho",
;;  "snippet" {"textOriginal" "Pewds didn't pass the 10 min.\nNice!",
;;  "updatedAt" "2017-02-18T19:49:27.000Z",
;;  "authorProfileImageUrl" "https://yt3.ggpht.com/-VUcC4gTAXKw/AAAAAAAAAAI/AAAAAAAAAAA/Ioq5UCPhl3U/s28-c-k-no-mo-rj-c0xffffff/photo.jpg",
;;  "authorDisplayName" "Joel Hartman",
;;  "videoId" "d2dNb0wdJF0",
;;  "textDisplay" "Pewds didn&#39;t pass the 10 min.<br />Nice!",
;;  "authorChannelUrl" "http://www.youtube.com/channel/UCR6O-2axWSGhJByWN0nrtJA",
;;  "canRate" false,
;;  "authorChannelId" {"value" "UCR6O-2axWSGhJByWN0nrtJA"},
;;  "likeCount" 0,
;;  "viewerRating" "none",
;;  "publishedAt" "2017-02-18T19:49:27.000Z"}},
;;  "canReply" false,
;;  "totalReplyCount" 0,
;;  "isPublic" true}} {"kind" "youtube#commentThread",
;;  "etag" "\"uQc-MPTsstrHkQcRXL3IWLmeNsM/JYrx4nX_y60-v8uR48sjh0c-dpk\"",
;;  "id" "z13hurtjypejstvra04ci5wwvqubvzxwm24",
;;  "snippet" {"videoId" "d2dNb0wdJF0",
;;  "topLevelComment" {"kind" "youtube#comment",
;;  "etag" "\"uQc-MPTsstrHkQcRXL3IWLmeNsM/gHm-sa-BKgvcjZOmYvKmz4ii5OA\"",
;;  "id" "z13hurtjypejstvra04ci5wwvqubvzxwm24",
;;  "snippet" {"textOriginal" "He thought the grinch was a green bear",
;;  "updatedAt" "2017-02-18T19:49:25.000Z",
;;  "authorProfileImageUrl" "https://yt3.ggpht.com/-fY5erDp_w2s/AAAAAAAAAAI/AAAAAAAAAAA/McIdUqMaloo/s28-c-k-no-mo-rj-c0xffffff/photo.jpg",
;;  "authorDisplayName" "Pyro Jack",
;;  "videoId" "d2dNb0wdJF0",
;;  "textDisplay" "He thought the grinch was a green bear",
;;  "authorChannelUrl" "http://www.youtube.com/channel/UCpoqw8wnYoRZ2dRy33CYc5g",
;;  "canRate" false,
;;  "authorChannelId" {"value" "UCpoqw8wnYoRZ2dRy33CYc5g"},
;;  "likeCount" 0,
;;  "viewerRating" "none",
;;  "publishedAt" "2017-02-18T19:49:25.000Z"}},
;;  "canReply" false,
;;  "totalReplyCount" 0,
;;  "isPublic" true}} {"kind" "youtube#commentThread",
;;  "etag" "\"uQc-MPTsstrHkQcRXL3IWLmeNsM/FznlI6Cbcq2bl91OxawulNEmK_0\"",
;;  "id" "z13xsdoynkyzhpyzm04ccduivvfss5h5j44",
;;  "snippet" {"videoId" "d2dNb0wdJF0",
;;  "topLevelComment" {"kind" "youtube#comment",
;;  "etag" "\"uQc-MPTsstrHkQcRXL3IWLmeNsM/nxEq7iPVbCUBtNh76rZg82quhBc\"",
;;  "id" "z13xsdoynkyzhpyzm04ccduivvfss5h5j44",
;;  "snippet" {"textOriginal" "lol finnish cut away song",
;;  "updatedAt" "2017-02-18T19:49:24.000Z",
;;  "authorProfileImageUrl" "https://yt3.ggpht.com/-oYpAq3DCuB8/AAAAAAAAAAI/AAAAAAAAAAA/ze-xmg-Ywx4/s28-c-k-no-mo-rj-c0xffffff/photo.jpg",
;;  "authorDisplayName" "Raimo Koskisalo",
;;  "videoId" "d2dNb0wdJF0",
;;  "textDisplay" "lol finnish cut away song",
;;  "authorChannelUrl" "http://www.youtube.com/channel/UCl3a204DYpRp0VRpADW8W3Q",
;;  "canRate" false,
;;  "authorChannelId" {"value" "UCl3a204DYpRp0VRpADW8W3Q"},
;;  "likeCount" 0,
;;  "viewerRating" "none",
;;  "publishedAt" "2017-02-18T19:49:24.000Z"}},
;;  "canReply" false,
;;  "totalReplyCount" 0,
;;  "isPublic" true}} {"kind" "youtube#commentThread",
;;  "etag" "\"uQc-MPTsstrHkQcRXL3IWLmeNsM/mQN8XKVbJ1MeA9Kqu3kgjFpSAP4\"",
;;  "id" "z13yt5dr1oypzdplo23hztmppkedftkyh04",
;;  "snippet" {"videoId" "d2dNb0wdJF0",
;;  "topLevelComment" {"kind" "youtube#comment",
;;  "etag" "\"uQc-MPTsstrHkQcRXL3IWLmeNsM/P66H2WKq_NT9wbV4gMxKIsmeyQg\"",
;;  "id" "z13yt5dr1oypzdplo23hztmppkedftkyh04",
;;  "snippet" {"textOriginal" "I didn't laugh because it wasn't funny but great video👍",
;;  "updatedAt" "2017-02-18T19:49:23.000Z",
;;  "authorProfileImageUrl" "https://yt3.ggpht.com/-WET-8u2zQ1g/AAAAAAAAAAI/AAAAAAAAAAA/woa2i8DaBSQ/s28-c-k-no-mo-rj-c0xffffff/photo.jpg",
;;  "authorDisplayName" "Aasif Ismail",
;;  "videoId" "d2dNb0wdJF0",
;;  "textDisplay" "I didn&#39;t laugh because it wasn&#39;t funny but great video👍",
;;  "authorChannelUrl" "http://www.youtube.com/channel/UCCMMiVnmxGDTaVQwL-BH9Mg",
;;  "canRate" false,
;;  "authorChannelId" {"value" "UCCMMiVnmxGDTaVQwL-BH9Mg"},
;;  "likeCount" 0,
;;  "viewerRating" "none",
;;  "publishedAt" "2017-02-18T19:49:23.000Z"}},
;;  "canReply" false,
;;  "totalReplyCount" 0,
;;  "isPublic" true}} {"kind" "youtube#commentThread",
;;  "etag" "\"uQc-MPTsstrHkQcRXL3IWLmeNsM/83R6jp3ghfzNBCs6_jcXU4gfCWs\"",
;;  "id" "z12ttrnycunpc1xqe04cjnorelels1moesc0k",
;;  "snippet" {"videoId" "d2dNb0wdJF0",
;;  "topLevelComment" {"kind" "youtube#comment",
;;  "etag" "\"uQc-MPTsstrHkQcRXL3IWLmeNsM/EgrVU0kRgXxaDLXu6nvDVxtAk0c\"",
;;  "id" "z12ttrnycunpc1xqe04cjnorelels1moesc0k",
;;  "snippet" {"textOriginal" "Video audio was a bit too soft pewds",
;;  "updatedAt" "2017-02-18T19:49:21.000Z",
;;  "authorProfileImageUrl" "https://yt3.ggpht.com/-_je2BcukIAs/AAAAAAAAAAI/AAAAAAAAAAA/ZhGu6d8Qg4k/s28-c-k-no-mo-rj-c0xffffff/photo.jpg",
;;  "authorDisplayName" "joshua Kon",
;;  "videoId" "d2dNb0wdJF0",
;;  "textDisplay" "Video audio was a bit too soft pewds",
;;  "authorChannelUrl" "http://www.youtube.com/channel/UCx0qWyoport8A3gyYX1If-A",
;;  "canRate" false,
;;  "authorChannelId" {"value" "UCx0qWyoport8A3gyYX1If-A"},
;;  "likeCount" 0,
;;  "viewerRating" "none",
;;  "publishedAt" "2017-02-18T19:49:21.000Z"}},
;;  "canReply" false,
;;  "totalReplyCount" 0,
;;  "isPublic" true}} {"kind" "youtube#commentThread",
;;  "etag" "\"uQc-MPTsstrHkQcRXL3IWLmeNsM/Bchzwd7hvwHbbgc0u2WmHwUT6fI\"",
;;  "id" "z131tbiz0wqlx3var04ccbrb4lq5un154hg",
;;  "snippet" {"videoId" "d2dNb0wdJF0",
;;  "topLevelComment" {"kind" "youtube#comment",
;;  "etag" "\"uQc-MPTsstrHkQcRXL3IWLmeNsM/FeGnzKa1j9rps-u5rICmJhbVCcI\"",
;;  "id" "z131tbiz0wqlx3var04ccbrb4lq5un154hg",
;;  "snippet" {"textOriginal" "Leave our memes on /gif/ alone and stick to normie reddit memes",
;;  "updatedAt" "2017-02-18T19:49:21.000Z",
;;  "authorProfileImageUrl" "https://yt3.ggpht.com/-eFzFVzxt2p8/AAAAAAAAAAI/AAAAAAAAAAA/vx1xTYvgEC0/s28-c-k-no-mo-rj-c0xffffff/photo.jpg",
;;  "authorDisplayName" "My Name Isn't Sam",
;;  "videoId" "d2dNb0wdJF0",
;;  "textDisplay" "Leave our memes on /gif/ alone and stick to normie reddit memes",
;;  "authorChannelUrl" "http://www.youtube.com/channel/UCtXyqj-QvyuHcj6Tk9UNyJg",
;;  "canRate" false,
;;  "authorChannelId" {"value" "UCtXyqj-QvyuHcj6Tk9UNyJg"},
;;  "likeCount" 0,
;;  "viewerRating" "none",
;;  "publishedAt" "2017-02-18T19:49:21.000Z"}},
;;  "canReply" false,
;;  "totalReplyCount" 0,
;;  "isPublic" true}} {"kind" "youtube#commentThread",
;;  "etag" "\"uQc-MPTsstrHkQcRXL3IWLmeNsM/nsn23AWut4bOatt3R96xNO76KbA\"",
;;  "id" "z122ev1i1taftdnhn04cibpxptfrghmaaro0k",
;;  "snippet" {"videoId" "d2dNb0wdJF0",
;;  "topLevelComment" {"kind" "youtube#comment",
;;  "etag" "\"uQc-MPTsstrHkQcRXL3IWLmeNsM/k4SSwLWVH4qdA1I9-RopV4KMQPQ\"",
;;  "id" "z122ev1i1taftdnhn04cibpxptfrghmaaro0k",
;;  "snippet" {"textOriginal" "what is the background music?",
;;  "updatedAt" "2017-02-18T19:49:18.000Z",
;;  "authorProfileImageUrl" "https://yt3.ggpht.com/-Pswl7BWCTQA/AAAAAAAAAAI/AAAAAAAAAAA/p3IZmQYih6c/s28-c-k-no-mo-rj-c0xffffff/photo.jpg",
;;  "authorDisplayName" "blahblahblah6496",
;;  "videoId" "d2dNb0wdJF0",
;;  "textDisplay" "what is the background music?",
;;  "authorChannelUrl" "http://www.youtube.com/channel/UCpjlvJuMysfolKZO5Ul1Nlg",
;;  "canRate" false,
;;  "authorChannelId" {"value" "UCpjlvJuMysfolKZO5Ul1Nlg"},
;;  "likeCount" 0,
;;  "viewerRating" "none",
;;  "publishedAt" "2017-02-18T19:49:18.000Z"}},
;;  "canReply" false,
;;  "totalReplyCount" 0,
;;  "isPublic" true}} {"kind" "youtube#commentThread",
;;  "etag" "\"uQc-MPTsstrHkQcRXL3IWLmeNsM/mBwLQowHabNXajkg-OrjzMaCprs\"",
;;  "id" "z12ttfkaesfkyzaut23nexgygmyaj1w2t",
;;  "snippet" {"videoId" "d2dNb0wdJF0",
;;  "topLevelComment" {"kind" "youtube#comment",
;;  "etag" "\"uQc-MPTsstrHkQcRXL3IWLmeNsM/nY3URBvSXZz5QCNCl_OnEL4x51k\"",
;;  "id" "z12ttfkaesfkyzaut23nexgygmyaj1w2t",
;;  "snippet" {"textOriginal" "I lost😂😂",
;;  "updatedAt" "2017-02-18T19:49:15.000Z",
;;  "authorProfileImageUrl" "https://yt3.ggpht.com/-psRF3E4SsYA/AAAAAAAAAAI/AAAAAAAAAAA/NkSK-oM2YK0/s28-c-k-no-mo-rj-c0xffffff/photo.jpg",
;;  "authorDisplayName" "Brianna Calvert-price",
;;  "videoId" "d2dNb0wdJF0",
;;  "textDisplay" "I lost😂😂",
;;  "authorChannelUrl" "http://www.youtube.com/channel/UC0LcnLGuk7KNNwHrRogPy3w",
;;  "canRate" false,
;;  "authorChannelId" {"value" "UC0LcnLGuk7KNNwHrRogPy3w"},
;;  "likeCount" 0,
;;  "viewerRating" "none",
;;  "publishedAt" "2017-02-18T19:49:15.000Z"}},
;;  "canReply" false,
;;  "totalReplyCount" 0,
;;  "isPublic" true}} {"kind" "youtube#commentThread",
;;  "etag" "\"uQc-MPTsstrHkQcRXL3IWLmeNsM/fSNk6hBC5eX3PrnP3BndutKCAw8\"",
;;  "id" "z132etwxsrbxslnam04cfbmitpaqv55hzr40k",
;;  "snippet" {"videoId" "d2dNb0wdJF0",
;;  "topLevelComment" {"kind" "youtube#comment",
;;  "etag" "\"uQc-MPTsstrHkQcRXL3IWLmeNsM/GC8j-U7Id8z-1HXjoEb5mz0zYVM\"",
;;  "id" "z132etwxsrbxslnam04cfbmitpaqv55hzr40k",
;;  "snippet" {"textOriginal" "And thus SovietWomble got a huge Sub Boost from Pewds after he was all pissy(jokingly) on his latest Bullshittery Video about PewDiePie thumbnails.",
;;  "updatedAt" "2017-02-18T19:49:14.000Z",
;;  "authorProfileImageUrl" "https://yt3.ggpht.com/-Rq2OUGBgekA/AAAAAAAAAAI/AAAAAAAAAAA/ykj-BTCzZb4/s28-c-k-no-mo-rj-c0xffffff/photo.jpg",
;;  "authorDisplayName" "OreMiner64",
;;  "videoId" "d2dNb0wdJF0",
;;  "textDisplay" "And thus SovietWomble got a huge Sub Boost from Pewds after he was all pissy(jokingly) on his latest Bullshittery Video about PewDiePie thumbnails.",
;;  "authorChannelUrl" "http://www.youtube.com/channel/UCY1uM5NYqvM4MYIXE5u6goA",
;;  "canRate" false,
;;  "authorChannelId" {"value" "UCY1uM5NYqvM4MYIXE5u6goA"},
;;  "likeCount" 0,
;;  "viewerRating" "none",
;;  "publishedAt" "2017-02-18T19:49:14.000Z"}},
;;  "canReply" false,
;;  "totalReplyCount" 0,
;;  "isPublic" true}} {"kind" "youtube#commentThread",
;;  "etag" "\"uQc-MPTsstrHkQcRXL3IWLmeNsM/-_F4u7wD6cEc98zhRGwpSBRXYh8\"",
;;  "id" "z12ze5xxjwritd2l223bedgjzkydwnlup",
;;  "snippet" {"videoId" "d2dNb0wdJF0",
;;  "topLevelComment" {"kind" "youtube#comment",
;;  "etag" "\"uQc-MPTsstrHkQcRXL3IWLmeNsM/QrhChuWnfRZehfjbZN8My5VBlYI\"",
;;  "id" "z12ze5xxjwritd2l223bedgjzkydwnlup",
;;  "snippet" {"textOriginal" "to make the fly do that\nyou need to spray it with fly spray\nit's dying",
;;  "updatedAt" "2017-02-18T19:49:12.000Z",
;;  "authorProfileImageUrl" "https://yt3.ggpht.com/-GRKnGGfxX8g/AAAAAAAAAAI/AAAAAAAAAAA/KlkHaqLjWIs/s28-c-k-no-mo-rj-c0xffffff/photo.jpg",
;;  "authorDisplayName" "Sharkyzane231",
;;  "videoId" "d2dNb0wdJF0",
;;  "textDisplay" "to make the fly do that<br />you need to spray it with fly spray<br />it&#39;s dying",
;;  "authorChannelUrl" "http://www.youtube.com/channel/UCBWVzKPdrwkmePhM1X4dsKg",
;;  "canRate" false,
;;  "authorChannelId" {"value" "UCBWVzKPdrwkmePhM1X4dsKg"},
;;  "likeCount" 0,
;;  "viewerRating" "none",
;;  "publishedAt" "2017-02-18T19:49:12.000Z"}},
;;  "canReply" false,
;;  "totalReplyCount" 0,
;;  "isPublic" true}} {"kind" "youtube#commentThread",
;;  "etag" "\"uQc-MPTsstrHkQcRXL3IWLmeNsM/na24Hh6ObP1rhpQtIl_I4k4tnwI\"",
;;  "id" "z131tfvykm20xbtgw04cfjqgww30cvfb4z40k",
;;  "snippet" {"videoId" "d2dNb0wdJF0",
;;  "topLevelComment" {"kind" "youtube#comment",
;;  "etag" "\"uQc-MPTsstrHkQcRXL3IWLmeNsM/QQG24Nh2_rWQnHaYn5g038qj4LI\"",
;;  "id" "z131tfvykm20xbtgw04cfjqgww30cvfb4z40k",
;;  "snippet" {"textOriginal" "Nice Freestyler tune!",
;;  "updatedAt" "2017-02-18T19:49:09.000Z",
;;  "authorProfileImageUrl" "https://yt3.ggpht.com/-4NtI2TNy_QE/AAAAAAAAAAI/AAAAAAAAAAA/yXoNDS97_xU/s28-c-k-no-mo-rj-c0xffffff/photo.jpg",
;;  "authorDisplayName" "Nicolás Carter",
;;  "videoId" "d2dNb0wdJF0",
;;  "textDisplay" "Nice Freestyler tune!",
;;  "authorChannelUrl" "http://www.youtube.com/channel/UCl59Fd_mjTrZ6mATnQm9EcA",
;;  "canRate" false,
;;  "authorChannelId" {"value" "UCl59Fd_mjTrZ6mATnQm9EcA"},
;;  "likeCount" 0,
;;  "viewerRating" "none",
;;  "publishedAt" "2017-02-18T19:49:09.000Z"}},
;;  "canReply" false,
;;  "totalReplyCount" 0,
;;  "isPublic" true}} {"kind" "youtube#commentThread",
;;  "etag" "\"uQc-MPTsstrHkQcRXL3IWLmeNsM/A0wr7puPRPm1TZNDju9ZNRBJqFw\"",
;;  "id" "z135hvowipn5z5xsf23pv3t55lbjy5vyj04",
;;  "snippet" {"videoId" "d2dNb0wdJF0",
;;  "topLevelComment" {"kind" "youtube#comment",
;;  "etag" "\"uQc-MPTsstrHkQcRXL3IWLmeNsM/aBVpjlPJOrPn9qkrmJSr5ELUSnE\"",
;;  "id" "z135hvowipn5z5xsf23pv3t55lbjy5vyj04",
;;  "snippet" {"textOriginal" "5:56 good old sovietwomble",
;;  "updatedAt" "2017-02-18T19:49:08.000Z",
;;  "authorProfileImageUrl" "https://yt3.ggpht.com/-b_UHlUk5FDU/AAAAAAAAAAI/AAAAAAAAAAA/c1G7KIotssM/s28-c-k-no-mo-rj-c0xffffff/photo.jpg",
;;  "authorDisplayName" "Ben Leicester",
;;  "videoId" "d2dNb0wdJF0",
;;  "textDisplay" "<a href=\"http://www.youtube.com/watch?v=d2dNb0wdJF0&amp;t=5m56s\">5:56</a> good old sovietwomble",
;;  "authorChannelUrl" "http://www.youtube.com/channel/UC0R1S-wULSGuxGI5Wef5DIg",
;;  "canRate" false,
;;  "authorChannelId" {"value" "UC0R1S-wULSGuxGI5Wef5DIg"},
;;  "likeCount" 0,
;;  "viewerRating" "none",
;;  "publishedAt" "2017-02-18T19:49:08.000Z"}},
;;  "canReply" false,
;;  "totalReplyCount" 0,
;;  "isPublic" true}} {"kind" "youtube#commentThread",
;;  "etag" "\"uQc-MPTsstrHkQcRXL3IWLmeNsM/yaECqHuaQx8fW_g2jqeaSGWnV1g\"",
;;  "id" "z130u5spnkvsubd3x23egfqw5luhtf2xo04",
;;  "snippet" {"videoId" "d2dNb0wdJF0",
;;  "topLevelComment" {"kind" "youtube#comment",
;;  "etag" "\"uQc-MPTsstrHkQcRXL3IWLmeNsM/mkq8IMOhOanoUYU9_uhM1s0YK-c\"",
;;  "id" "z130u5spnkvsubd3x23egfqw5luhtf2xo04",
;;  "snippet" {"textOriginal" "09:59 thanks me later.\nit make me feel bad to  miss the ads.",
;;  "updatedAt" "2017-02-18T19:49:33.000Z",
;;  "authorProfileImageUrl" "https://yt3.ggpht.com/-toUUL0Mf1Io/AAAAAAAAAAI/AAAAAAAAAAA/Y9TEC392rAw/s28-c-k-no-mo-rj-c0xffffff/photo.jpg",
;;  "authorDisplayName" "ToTryChannel",
;;  "videoId" "d2dNb0wdJF0",
;;  "textDisplay" "<a href=\"https://www.youtube.com/watch?v=d2dNb0wdJF0&amp;t=09m59s\">09:59</a> thanks me later.<br />it make me feel bad to  miss the ads.",
;;  "authorChannelUrl" "http://www.youtube.com/channel/UCuSc_7mc3GnX1Z6CWbwE-tA",
;;  "canRate" false,
;;  "authorChannelId" {"value" "UCuSc_7mc3GnX1Z6CWbwE-tA"},
;;  "likeCount" 0,
;;  "viewerRating" "none",
;;  "publishedAt" "2017-02-18T19:49:07.000Z"}},
;;  "canReply" false,
;;  "totalReplyCount" 0,
;;  "isPublic" true}} {"kind" "youtube#commentThread",
;;  "etag" "\"uQc-MPTsstrHkQcRXL3IWLmeNsM/sF1gYMj5tB60aVTYgN5hHwE_Ctw\"",
;;  "id" "z123ffqinyuaxdtm022wf3wxqx30cxbvi",
;;  "snippet" {"videoId" "d2dNb0wdJF0",
;;  "topLevelComment" {"kind" "youtube#comment",
;;  "etag" "\"uQc-MPTsstrHkQcRXL3IWLmeNsM/FYu7y-qMQsViEZcS3NcvsRmTGX0\"",
;;  "id" "z123ffqinyuaxdtm022wf3wxqx30cxbvi",
;;  "snippet" {"textOriginal" "Does he not know who the grinch is",
;;  "updatedAt" "2017-02-18T19:49:06.000Z",
;;  "authorProfileImageUrl" "https://yt3.ggpht.com/-NJMlfbC1Udw/AAAAAAAAAAI/AAAAAAAAAAA/9jQpOMV8ZeY/s28-c-k-no-mo-rj-c0xffffff/photo.jpg",
;;  "authorDisplayName" "Orlando Saldivar",
;;  "videoId" "d2dNb0wdJF0",
;;  "textDisplay" "Does he not know who the grinch is",
;;  "authorChannelUrl" "http://www.youtube.com/channel/UCLCmX-BCUa_xloiAuhfqj6Q",
;;  "canRate" false,
;;  "authorChannelId" {"value" "UCLCmX-BCUa_xloiAuhfqj6Q"},
;;  "likeCount" 0,
;;  "viewerRating" "none",
;;  "publishedAt" "2017-02-18T19:49:06.000Z"}},
;;  "canReply" false,
;;  "totalReplyCount" 0,
;;  "isPublic" true}} {"kind" "youtube#commentThread",
;;  "etag" "\"uQc-MPTsstrHkQcRXL3IWLmeNsM/HMyiLFoHSe5IfJHr6jV6i4LkiJM\"",
;;  "id" "z12of3frnsuoshtne222idtxhsyxsvrvw",
;;  "snippet" {"videoId" "d2dNb0wdJF0",
;;  "topLevelComment" {"kind" "youtube#comment",
;;  "etag" "\"uQc-MPTsstrHkQcRXL3IWLmeNsM/t7KUPSHSu4Z7l7SDBtbrZXrLqtQ\"",
;;  "id" "z12of3frnsuoshtne222idtxhsyxsvrvw",
;;  "snippet" {"textOriginal" "\"I think police should stick to attacking innocents\" time for him to lose everything over a joke again",
;;  "updatedAt" "2017-02-18T19:49:00.000Z",
;;  "authorProfileImageUrl" "https://yt3.ggpht.com/-H7r1i1AW5kU/AAAAAAAAAAI/AAAAAAAAAAA/NSzXQV1Yc1w/s28-c-k-no-mo-rj-c0xffffff/photo.jpg",
;;  "authorDisplayName" "Sans Skeleton",
;;  "videoId" "d2dNb0wdJF0",
;;  "textDisplay" "&quot;I think police should stick to attacking innocents&quot; time for him to lose everything over a joke again",
;;  "authorChannelUrl" "http://www.youtube.com/channel/UCsl4jXh9vhPPGkWBM6uYsMw",
;;  "canRate" false,
;;  "authorChannelId" {"value" "UCsl4jXh9vhPPGkWBM6uYsMw"},
;;  "likeCount" 0,
;;  "viewerRating" "none",
;;  "publishedAt" "2017-02-18T19:49:00.000Z"}},
;;  "canReply" false,
;;  "totalReplyCount" 0,
;;  "isPublic" true}} {"kind" "youtube#commentThread",
;;  "etag" "\"uQc-MPTsstrHkQcRXL3IWLmeNsM/Hg0oncRgFA-ZjNeb-_-42oZtc9A\"",
;;  "id" "z135cdgbftnovd3q304ccvxa3myaihqavvo0k",
;;  "snippet" {"videoId" "d2dNb0wdJF0",
;;  "topLevelComment" {"kind" "youtube#comment",
;;  "etag" "\"uQc-MPTsstrHkQcRXL3IWLmeNsM/xkJJiaWhI-RRqb3kd2dHlAKOsv8\"",
;;  "id" "z135cdgbftnovd3q304ccvxa3myaihqavvo0k",
;;  "snippet" {"textOriginal" "ok I lost at sponge bob and mickey beating up the dude",
;;  "updatedAt" "2017-02-18T19:49:00.000Z",
;;  "authorProfileImageUrl" "https://yt3.ggpht.com/-g8K237l5VrM/AAAAAAAAAAI/AAAAAAAAAAA/TatushhO38U/s28-c-k-no-mo-rj-c0xffffff/photo.jpg",
;;  "authorDisplayName" "niko b",
;;  "videoId" "d2dNb0wdJF0",
;;  "textDisplay" "ok I lost at sponge bob and mickey beating up the dude",
;;  "authorChannelUrl" "http://www.youtube.com/channel/UCjvrbf2EMRGN2Pl_vw9vqLg",
;;  "canRate" false,
;;  "authorChannelId" {"value" "UCjvrbf2EMRGN2Pl_vw9vqLg"},
;;  "likeCount" 0,
;;  "viewerRating" "none",
;;  "publishedAt" "2017-02-18T19:49:00.000Z"}},
;;  "canReply" false,
;;  "totalReplyCount" 0,
;;  "isPublic" true}} {"kind" "youtube#commentThread",
;;  "etag" "\"uQc-MPTsstrHkQcRXL3IWLmeNsM/6rzPsBSlGgYUbsFdaNT1W8lYb5g\"",
;;  "id" "z13xvl2ovsqtiz1wj04cfrpxgkmwhvb5zok0k",
;;  "snippet" {"videoId" "d2dNb0wdJF0",
;;  "topLevelComment" {"kind" "youtube#comment",
;;  "etag" "\"uQc-MPTsstrHkQcRXL3IWLmeNsM/vO1aAbS4eSd6Z2Swj-6PS4KtZ-c\"",
;;  "id" "z13xvl2ovsqtiz1wj04cfrpxgkmwhvb5zok0k",
;;  "snippet" {"textOriginal" "This is Bob - 🐨\nBob has no penis\n1 like = 1 penis 🍆",
;;  "updatedAt" "2017-02-18T19:48:59.000Z",
;;  "authorProfileImageUrl" "https://yt3.ggpht.com/-1FlFWIb0gFY/AAAAAAAAAAI/AAAAAAAAAAA/mFk1cYFrwxM/s28-c-k-no-mo-rj-c0xffffff/photo.jpg",
;;  "authorDisplayName" "iArvin",
;;  "videoId" "d2dNb0wdJF0",
;;  "textDisplay" "This is Bob - 🐨<br />Bob has no penis<br />1 like = 1 penis 🍆",
;;  "authorChannelUrl" "http://www.youtube.com/channel/UC7eXcXvkGEoYpjjYv2hrqxA",
;;  "canRate" false,
;;  "authorChannelId" {"value" "UC7eXcXvkGEoYpjjYv2hrqxA"},
;;  "likeCount" 0,
;;  "viewerRating" "none",
;;  "publishedAt" "2017-02-18T19:48:59.000Z"}},
;;  "canReply" false,
;;  "totalReplyCount" 0,
;;  "isPublic" true}} {"kind" "youtube#commentThread",
;;  "etag" "\"uQc-MPTsstrHkQcRXL3IWLmeNsM/1Dn25Gd_MVoW1IWVobV4AvRgxlo\"",
;;  "id" "z12dwv2h2t3bwjznt22zhjliux3dj3ok2",
;;  "snippet" {"videoId" "d2dNb0wdJF0",
;;  "topLevelComment" {"kind" "youtube#comment",
;;  "etag" "\"uQc-MPTsstrHkQcRXL3IWLmeNsM/VY23nVea6R7TZfHeU8fxZFFVAUM\"",
;;  "id" "z12dwv2h2t3bwjznt22zhjliux3dj3ok2",
;;  "snippet" {"textOriginal" "I want to see pewds in pink hair",
;;  "updatedAt" "2017-02-18T19:48:59.000Z",
;;  "authorProfileImageUrl" "https://yt3.ggpht.com/-fAtfgiT3Emo/AAAAAAAAAAI/AAAAAAAAAAA/6nWf1hmIirA/s28-c-k-no-mo-rj-c0xffffff/photo.jpg",
;;  "authorDisplayName" "Songya Ni",
;;  "videoId" "d2dNb0wdJF0",
;;  "textDisplay" "I want to see pewds in pink hair",
;;  "authorChannelUrl" "http://www.youtube.com/channel/UCghzxs3qTN5TzQCCXwt5J-g",
;;  "canRate" false,
;;  "authorChannelId" {"value" "UCghzxs3qTN5TzQCCXwt5J-g"},
;;  "likeCount" 0,
;;  "viewerRating" "none",
;;  "publishedAt" "2017-02-18T19:48:59.000Z"}},
;;  "canReply" false,
;;  "totalReplyCount" 0,
;;  "isPublic" true}}]
