(ns thehex.notube.util)

(defn with-abs-path
  "Gets absolute path of server file."
  [filename]
  (str (.getCanonicalPath (clojure.java.io/file ".")) (java.io.File/separator) filename))

(println "loading... notubes util yo!")
