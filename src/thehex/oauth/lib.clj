(ns thehex.oauth.lib
  (:require [org.httpkit.client :as http]
            [clojure.java.browse :as browser]
            [clojure.core.async :as async :refer [<! >! <!! >!! timeout chan alt! go]]
            [org.httpkit.server :as web]
            [compojure.route :as route] [compojure.handler :as handler]
            [compojure.core :as compojure]
            [clojure.edn :as edn]
            [taoensso.timbre :as log]
            [clojure.data.json :as json]
            [slingshot.slingshot :refer [try+]]
            ;; internal
            [thehex.notube.config :as config]
            [thehex.notube.util :as util])
  (:import [java.net URLEncoder]))

(def server-port
  (let [port (util/read-config :port)]
    (if port
      (try
        (Integer/parseInt port)
        (catch java.lang.ClassCastException e
          port))
      8889)))

(def oauth2-params
  {:client-id (or (System/getenv "YOUTUBE_CLIENT_ID")
                  (util/read-config :youtube-client-id))
   :client-secret (or (System/getenv "YOUTUBE_CLIENT_SECRET")
                      (util/read-config :youtube-client-secret))
   :authorize-uri  "https://accounts.google.com/o/oauth2/auth"
   ;; google will append code= grab this query param on server
   :redirect-uri (format "http://127.0.0.1:%s/oauth2" server-port) ;; or #error hash if failed or not authorized
   :access-token-uri "https://accounts.google.com/o/oauth2/token"
   :scope "https://www.googleapis.com/auth/youtube.force-ssl"})

(defonce server (atom nil))
(def creds-chan (chan))

(declare stop-server!)

;; we have a token map here, and a token map param when we persist. maybe we can remove and stop using this?
(def token-map (atom nil))

(compojure/defroutes all-routes
  (compojure/GET "/" [] "Hello, World!")
  (compojure/GET "/oauth2" {params :query-params}
    (log/debug "handling oauth2 endpoint...")
    (let [code (get params "code")]
      (log/debug "got code on /oauth2 endpoint")
      ;; TODO: set timeout for if user doesnt authenticate? can then stop all this (close channel) and stop server
      (do
        (log/trace (str "Putting code in creds-chan inside async/go: " code))
        (if (nil? code)
          (do
            (stop-server!)
            (System/exit 2))
          (>!! creds-chan code))
        (log/trace "Finished putting code in creds-chan inside async/go"))
      (log/debug (str "Code from oauth2: " code))
      ;; TODO: close browser? or do we leave user there doing nothing?
      ;; if browse-url cant close browser- can we use something like selenium?
      "Authentication Succeeded."))) ;; Return something to browser body

(defn authorization-uri
  "Create authorization uri from params"
  [client-params]
  (str
   (:authorize-uri client-params)
   "?response_type=code"
   "&client_id="
   (URLEncoder/encode (:client-id client-params))
   "&redirect_uri="
   (URLEncoder/encode (:redirect-uri client-params))
   "&scope="
   (URLEncoder/encode (:scope client-params))
   "&accesstype=offline"))

(defn start-server!
  ""
  []
  (log/infof "Starting Server on port %s to catch oauth code." server-port)
  (reset! server (web/run-server (handler/site #'all-routes) {:port server-port})))

(defn stop-server!
  "Gracefully shutdown server: wait 100ms for existing requests to be finished
   :timeout is optional, when no timeout, stop immediately"
  []
  (log/warn "Stopping server...")
  (when-not (nil? @server)
    (@server :timeout 100)
    (reset! server nil))
  (log/warn "Server stopped."))

(defn fetch-tokens!
  "
  Accepts a code from youtube, and returns this json if authentication is valid:
  {
    'access_token' : 'ya29.AHES6ZTtm7SuokEB-RGtbBty9IIlNiP9-eNMMQKtXdMP3sfjL1Fc',
    'token_type' : 'Bearer',
    'expires_in' : 3600,
    'refresh_token' : '1/HKSmLFXzqP0leUihZp2xUt3-5wkU7Gmu2Os_eBnzw74'
  }

   Your application should store both values in a secure, long-lived location that is accessible between different invocations of your application.
   The refresh token enables your application to obtain a new access token if the one that you have expires.
   As such, if your application loses the refresh token, the user will need to repeat the OAuth 2.0 consent flow so that your application can obtain a new refresh token.
  Access tokens last 60 minutes (an hour...)
  "
  [code]
  (try
    (log/trace (str "Retrieving tokens using code: " code))
    (let [{:keys [status headers error body]} @(http/post (:access-token-uri oauth2-params)
                          {:form-params {:code        code
                                         :grant_type   "authorization_code"
                                         :client_id    (:client-id oauth2-params)
                                         :redirect_uri (:redirect-uri oauth2-params)
                                         :client_secret (:client-secret oauth2-params)}})]
      (log/debug (format "Status: %s \n Headers: %s \n error: %s \n" status headers error))
      (log/trace (str "Token response body: " body))
      body)
    (catch Exception e
      (log/error (str e)))))

(defn persist-tokens!
  "Persist tokens in tokens.edn file. Receives a stringified json."
  [tokens-map]
  (log/debug "Storing tokens map into tokens.edn")
  (log/debugf "Token map atom res: %s" tokens-map)
  (try (spit (util/with-abs-path "tokens.edn")
             (let [as-json (json/read-str tokens-map)]
               (str {:access-token (get as-json "access_token")
                     :refresh-token (get as-json "refresh_token")})))
       (catch java.lang.ClassCastException e
         (log/error e "Did not send a json string to persist!"))))

(defn read-persisted-tokens
  ""
  []
  (log/debug "Retrieving tokens map from tokens.edn")
  (try
    (edn/read-string (slurp (util/with-abs-path "tokens.edn")))
    (catch Exception e
      (log/info "\nUnable to load tokens. Please create tokens.edn file by popoulating with `-t p`. You can also manually copy a previously known tokens.edn file to project install dir.")
      (log/trace e))))

(defn populate-tokens!
  "Use most functions in this namespace to setup a server, start a browser session, authenticate users and setup tokens for use by api."
  []
  (log/info "Populating all tokens...")
  (start-server!)
  (browser/browse-url (authorization-uri oauth2-params))
  ;; TODO: set a timeout here just in case user doesnt log in
  (log/debug "Awaiting code from creds channel")
  (let [go-chan (go
                  (let [code (<! creds-chan)]
                    (log/trace "Got code from creds channel: " code)
                    (let [token-res (fetch-tokens! code)]
                      (if (get token-res "error")
                        (do
                          (log/error "Received error on token response..not updating nor persisting token map")
                          nil)
                        (persist-tokens! (reset! token-map token-res))))
                    (log/info "Populated tokens. Stopping server and exiting.")
                    (stop-server!)
                    (System/exit 0)))]
    (<!! go-chan) ;; needed to wait until go channel is finished
    ))

(defn refresh-tokens!
  "Request a new access token
  Note that there are limits on the number of refresh tokens that will be issued;
  one limit per client/user combination, and another per user across all clients.
  You should save refresh tokens in long-term storage and continue to use them as long as they remain valid.
  If your application requests too many refresh tokens, it may run into these limits,
  in which case older refresh tokens will stop working."
  []
  (let [refresh-token (get (read-persisted-tokens) :refresh-token)]
    (log/trace (str "Refreshing access token with refresh-token:" refresh-token))
    (try+
     (let [{:keys [status headers error body]} @(http/post (:access-token-uri oauth2-params)
                                                           {:form-params {:grant_type       "refresh_token"
                                                                          :refresh_token    refresh-token
                                                                          :client_id (:client-id oauth2-params)
                                                                          :client_secret (:client-secret oauth2-params)}})
           as-json (json/read-str body)]
       (log/debugf "status: %s, error: %s, body: %s" status error body)
       (log/debugf "Refresh-tokens response as-json: " as-json)
       (persist-tokens! (reset! token-map (json/write-str {"access_token" (get as-json "access_token")
                                                           "refresh_token" refresh-token}))))
     (catch [:status 401]
         ;; TODO: do something if refresh token has expired or otherwise lost...
         ;; maybe start the oauth login process again?
         e (log/errorf "Received unauthorized 401 while trying to refresh tokens: %s" e)))))

;; TODO: idea form website for later... macro to wrap all catch 401 unauthorized from youtube api oauth calls?
;; something like:
;; (defn endpoint-call
;;   ""
;;   [endpoint-url access-token refresh-token]
;;   (try+
;;    (and (endpoint-url access-token)
;;         [access-token refresh-token])
;;    (catch [:status 401] _ (refresh-tokens refresh-token))))
