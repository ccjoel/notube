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
                 [clj-http "3.4.1"] ;; TODO: replace with http-kit client code
                 [http-kit "2.2.0"] ;; for client and web server
                 [compojure "1.5.2"] ;; simple route to populate tokens from google login
                 [com.taoensso/timbre "4.8.0"]
                 [org.clojure/core.async "0.2.395"]
                 [org.clojure/tools.cli "0.3.5"]
                 [org.clojure/tools.nrepl "0.2.12"]
                 [org.clojure/data.json "0.2.6"] ;; parse json api responses
                 [slingshot "0.12.2"]])

(task-options!
 aot {:namespace   #{'thehex.notube.cli}}
 pom {:project     project
      :version     version
      :description "Notube crawls Youtube Channels and reports/deletes comments that include spam, bullying, harrassment, hate, and violent speech."
      :url         "FIXME"
      :scm         {:url "https://github.com/teh0xqb/notube"}
      :license     {"Eclipse Public License"
                    "http://www.eclipse.org/legal/epl-v10.html"}}
 jar {:main        'thehex.notube.cli
      :file        (str "notube-" version ".jar")})

(deftask build
  "Build the project locally as a production JAR.
   For dev, just run ./build.boot -h, etc"
  [d dir PATH #{str} "the set of directories to write to (target)."]
  (System/setProperty "prod" "true")
  (let [dir (if (seq dir) dir #{"target"})]
    (comp (aot) (pom) (uber) (jar) (target :dir dir))))

(require '[adzerk.boot-test :refer [test]])

(defn -main
  "This will run when executing this file directly i.e. ./build.boot
   dev version. accepts command line args"
  [& args]
  (require 'thehex.notube.cli)
  (apply (resolve 'thehex.notube.cli/-main) args))
