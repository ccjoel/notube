(ns thehex.notube.util)

(defn with-abs-path
  "Gets absolute path of server file."
  [filename]
  (str (.getCanonicalPath (clojure.java.io/file ".")) (java.io.File/separator) filename))

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
