#!/usr/bin/env boot

(def project 'notube)
(def version "0.1.2")

(set-env!
 :source-paths #{"src"}
 :resource-paths #{"resources"}
 :dependencies '[[org.clojure/clojure "1.8.0"]
                 [adzerk/boot-test "1.1.2" :scope "test"] ;; only needed for tests..ensure this is the case
                 [javax.servlet/servlet-api "2.5"] ;; needed for compojure/server routes
                 [http-kit "2.2.0"] ;; for client and web server
                 [compojure "1.5.2"] ;; simple route to populate tokens from google login
                 [com.taoensso/timbre "4.8.0"]
                 [org.clojure/core.async "0.2.395"]
                 [org.clojure/tools.cli "0.3.5"]
                 [org.clojure/data.json "0.2.6"] ;; parse json api responses
                 [slingshot "0.12.2"]])

(task-options!
 aot {:all true}
 jar {:main        'thehex.notube.cli
      :file        (str "notube-" version ".jar")})

(deftask uberjar
  "Build the project locally as a production JAR.
   For dev, just run ./build.boot -h, etc"
  [d dir PATH #{str} "the set of directories to write to (target)."]
  (System/setProperty "prod" "true")
  (let [dir (if (seq dir) dir #{"target"})]
    (comp (aot) (uber) (jar) (target :dir dir))))

(deftask devbuild
  []
  ;; override end and task options
  (set-env!
   :source-paths #{"src" "test"})
  (task-options!
   aot {:namespaces #{'thehex.notube.cli}})
  (uberjar))

(require '[adzerk.boot-test :refer [test]])

(defn -main
  "This will run when executing this file directly i.e. ./build.boot
   dev version. Accepts command line args"
  [& args]
  (require 'thehex.notube.cli)
  (apply (resolve 'thehex.notube.cli/-main) args))
