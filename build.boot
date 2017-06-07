#!/usr/bin/env boot

(def project 'notube)
(def version "0.1.1")

(set-env!
 :resource-paths #{"resources" "src"}
 :source-paths #{"test"}
 :dependencies '[[org.clojure/clojure "1.8.0"]
                 [adzerk/boot-test "1.1.2" :scope "test"]
                 [org.clojure/tools.nrepl "0.2.12"]
                 [javax.servlet/servlet-api "2.5"]
                 [ring/ring-devel "1.5.0"] ;; to restart server of each change for dev
                 [clj-http "3.4.1"] ;; replace with http-kit client code
                 [http-kit "2.2.0"] ;; for client and web server
                 [compojure "1.5.2"]
                 [com.taoensso/timbre "4.8.0"]
                 [org.clojure/core.async "0.2.395"]
                 [org.clojure/tools.nrepl "0.2.12"]
                 [org.clojure/data.json "0.2.6"] ;; parse json api responses
                 [slingshot "0.12.2"]])

(task-options!
 aot {:namespace   #{'thehex.notube.core}}
 pom {:project     project
      :version     version
      :description "Notube crawls Youtube Channels and reports/deletes comments that include spam, bullying, harrassment, hate, and violent speech."
      :url         "FIXME"
      :scm         {:url "https://github.com/teh0xqb/notube"}
      :license     {"Eclipse Public License"
                    "http://www.eclipse.org/legal/epl-v10.html"}}
 jar {:main        'thehex.notube.core
      :file        (str "notube-" version ".jar")})

(deftask build
  "Build the project locally as a JAR."
  [d dir PATH #{str} "the set of directories to write to (target)."]
  (let [dir (if (seq dir) dir #{"target"})]
    (comp (aot) (pom) (uber) (jar) (target :dir dir))))

(deftask access
  "Start server and prompt user, to store access token"
  [a args ARG [str] "the arguments for the application."]
  (require '[thehex.oauth.core :as app])
  (apply (resolve 'app/-main) args))

(deftask refresh
  "refresh access token"
  [a args ARG [str] "arguments for refresh"]
  (require '[thehex.oauth.core :as app])
  (apply (resolve 'app/refresh) args))

(require '[adzerk.boot-test :refer [test]])

(defn -main
  "This will run when executing this file directly i.e. ./build.boot"
  [& args]
  (require 'thehex.notube.core)
  (apply (resolve 'thehex.notube.core/-main) args))

(deftask run
  "Run the project."
  []
  (-main))
