(ns thehex.oauth.lib-test
  (:require [clojure.test :refer :all]
            [thehex.oauth.lib :refer :all]))

(deftest authorization-uri-test
  (testing "Given client-params returns correct auth uri"
    (is (= "https://accounts.google.com/o/oauth2/auth?response_type=code&client_id=an-id...&redirect_uri=http%3A%2F%2F127.0.0.1%3A8889%2Foauth2&scope=https%3A%2F%2Fwww.googleapis.com%2Fauth%2Fyoutube.force-ssl&accesstype=offline" (authorization-uri {:client-id "an-id..."
                                          :client-secret "a-secret-12345"
                                          :authorize-uri  "https://accounts.google.com/o/oauth2/auth"
                                          :redirect-uri "http://127.0.0.1:8889/oauth2" ;; google will append code= grab this query param on server
                                          ;; or #error hash if failed or not authorized
                                          :access-token-uri "https://accounts.google.com/o/oauth2/token"
                                          :scope "https://www.googleapis.com/auth/youtube.force-ssl"})))))



