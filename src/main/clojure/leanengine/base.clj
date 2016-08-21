(ns leanengine.base
  (:import (com.avos.avoscloud AVOSCloud AVException AVUser)
           (java.lang.reflect Field)
           (java.util Date)
           (java.security MessageDigest)))

(def ^:private error-fields
  (->> (.getFields AVException)
       (filter (fn [^Field f] (= "int" (str (.getType f)))))
       (map (fn [^Field f] [(.getInt f AVException) (.getName f)]))
       (into {})))

(defn avos-error-parse
  [^AVException error]
  (if-let [desc (error-fields (.getCode error))]
    (println (str "https://leancloud.cn/api-docs/android/com/avos/avoscloud/AVException.html#" desc))))

(defmacro avos-try
  "将表达式包入 try catch 块中,如发生错误返回错误信息"
  [& body]
  `(try
     ~@body
     (catch AVException e#
       (if-let [info# (avos-error-parse e#)]
         info#
         (.printStackTrace e#)))))

(defn now
  []
  (.getTime (Date.)))

(defn- digest
  [type ^String str]
  (let [bytes (.getBytes str)
        md (MessageDigest/getInstance type)]
    (-> (.digest md bytes)
        (^BigInteger (fn [^bytes bytes] (BigInteger. 1 bytes)))
        (.toString 16))))

(def md5 (partial digest "MD5"))

(defn get-sign
  ([]
    (get-sign :key))
  ([type]
    (if (= :master type)
      (let [now (now)
            sign (md5 (str now (System/getenv "LEANCLOUD_APP_MASTER_KEY")))]
        (str sign "," now ",master"))
      (let [now (now)
            sign (md5 (str now (System/getenv "LEANCLOUD_APP_KEY")))]
        (str sign "," now))))
  )
