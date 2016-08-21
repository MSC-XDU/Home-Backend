(ns leanengine.file
  (:import (com.avos.avoscloud AVFile)
           (java.io File))
  (:use leanengine.base
        clojure.tools.logging))

(defn save-file-bytes
  [^String name ^bytes byte]
  ^AVFile (let [file (AVFile. name byte)]
            (.save file)
            file))

(defn save-file-file
  [^String name ^File file]
  ^AVFile (let [file (AVFile/withFile name file)]
            (info file)
            (.save file)
            (info "success")
            file))