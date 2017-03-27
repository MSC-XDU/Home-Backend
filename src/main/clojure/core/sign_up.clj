(ns core.sign-up
  (:use leanengine.base
        leanengine.query
        leanengine.file
        leanengine.object
        clojure.tools.logging
        [clojure.data.json :only [write-str read-str]]
        [ring.util.codec :only (url-encode)]
        [medley.core :only (map-keys)])
  (:require [clj-http.client :as client])
  (:import [com.avos.avoscloud AVUser AVException AVFile AVObject]))

(defmacro ^:private wrap-try
  [& body]
  `(try
     ~@body
     (catch AVException e#
       (if (= 214 (.getCode e#))
         {:success? true :exist? true}
         {:success? false :error (.getMessage e#)}))))

(defn request-phone-verify
  [^String phone]
  (wrap-try
    (AVUser/requestMobilePhoneVerify phone)
    {:success? true}))

(defn verify-phone
  [^String code]
  (wrap-try
    (AVUser/verifyMobilePhone code)
    {:success? true}))

(defn sign-up
  [^String id ^String name ^String phone]
  (wrap-try
    (.getSessionToken (doto (AVUser.)
                        (.setUsername id)
                        (.setMobilePhoneNumber phone)
                        (.setPassword phone)
                        (.put "name" name)
                        (.signUp)))
    (future (request-phone-verify phone))
    {:success? true :exist? false}))