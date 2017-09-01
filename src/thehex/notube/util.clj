(ns thehex.notube.util
  (:require
   [clojure.edn :as edn]))

(declare read-config)

(def ^:const prod?
  (boolean (System/getProperty "prod")))

(defn with-abs-path
  "Gets absolute path of server file."
  [filename]
  (str
   (if prod?
     (read-config :install-dir)
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

(defmulti read-config
  "Simple utility fn to read a key from config.edn"
  (fn [& args]
    (when args
      (type (first args)))))

(defmethod read-config nil
  [& args]
  (try
    (edn/read-string (slurp (clojure.java.io/resource "config.edn")))
    (catch java.lang.IllegalArgumentException e
      (binding [*out* *err*]
        (println "Please copy config.sample.edn file into config.edn and set youtube api app settings.")))))

(defmethod read-config clojure.lang.Keyword
  [key]
  (-> (read-config) key))

(defmethod read-config java.lang.String
  [str]
  (read-config (keyword str)))
