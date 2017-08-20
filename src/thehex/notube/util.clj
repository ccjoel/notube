(ns thehex.notube.util
  (:require [clojure.edn :as edn]
            [taoensso.timbre :as log]))

(declare read-config-key)

(def ^:const prod?
  (boolean (System/getProperty "prod")))

(defn with-abs-path
  "Gets absolute path of server file."
  [filename]
  (str
   (if prod?
     (read-config-key :install-dir)
     (.getCanonicalPath (clojure.java.io/file ".")))
   (java.io.File/separator)
   filename))

(defn process-file-by-lines
  "Process file reading it line-by-line.
   If no process provided, will just print all. If process-fn
   provided, but no out fn, parses then prints. if both process
  and out fn provided, use those."
  ([file]
   (process-file-by-lines file identity))
  ([file process-fn]
   (process-file-by-lines file process-fn println))
  ([file process-fn output-fn]
   (with-open [rdr (clojure.java.io/reader file)]
     (doseq [line (line-seq rdr)]
       (output-fn
        (process-fn line))))))

(defn read-config-key
  "Simple utility fn to read a key from config.edn"
  [key]
  (get
   (edn/read-string (slurp (clojure.java.io/resource "config.edn")))
   key))
