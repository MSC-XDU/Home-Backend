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
       (error (.getCode e#))
       {:success? false :error (.getMessage e#)})))

(defn user-exist?
  "给定邮箱查看用户是否存在"
  [username]
  (try
    {:success? true
     :exist?   (-> (client/get "https://api.leancloud.cn/1.1/users"
                               {:headers      {"X-LC-Id"   (System/getenv "LEANCLOUD_APP_ID")
                                               "X-LC-Sign" (get-sign :master)}
                                :as           :json
                                :content-type :json
                                :accept       :json
                                :query-params {"where" (write-str {:username username})}})
                   (:body)
                   (:results)
                   (#(or (empty? %) (nil? %)))
                   (not))}
    (catch Exception e
      {:success? false})))

(defn sign-up
  [^String e-mail ^String password]
  (wrap-try
    (doto (AVUser.)
      (.setUsername e-mail)
      (.setEmail e-mail)
      (.setPassword password)
      (.signUp))
    {:success? true}))

(defn log-in
  [^String e-mail ^String password]
  (wrap-try
    (AVUser/logIn e-mail password)
    {:success? true}))

(defn set-phone
  [^String phone]
  (wrap-try
    (let [user (AVUser/getCurrentUser)]
      (if-not (.getBoolean (.fetch user) "mobilePhoneVerified")
        (do
          (.setMobilePhoneNumber user phone)
          (.save user)))
      {:success? true})))

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

(defn set-base-info
  [info]
  (wrap-try
    (let [phone (info "phone")
          info (dissoc info "phone")]
      (-> (AVUser/getCurrentUser)
          (put-object info)
          (save-object))
      (set-phone phone)
      {:success? true})))

(defn set-sign-up-info
  [info]
  (wrap-try
    (let [user (AVUser/getCurrentUser)
          forms (-> (avos-query "SignUp")
                    (query-object ["user" := user])
                    (query-find))]
      (if-let [form (first forms)]
        (do
          (-> (put-object form info)
              (save-object))
          {:success? true})
        (do
          (-> (avos-object "SignUp")
              (put-object info)
              (put-object ["user" user])
              (save-object))
          {:success? true})))))

(defn get-sign-up-info
  []
  (wrap-try
    (try
      (let [user (AVUser/getCurrentUser)
            info (-> (avos-query "SignUp")
                     (query-object ["user" := user])
                     (query-include "files")
                     (query-first))
            files (:files (get-slot info ["files" :seq]))
            files (map (fn [file] {:name    (file "originalName")
                                   :url     (file "url")
                                   :id      (file "objectId")
                                   :loading false}) files)]
        (-> info
            (.toJSONObject)
            (.toString)
            (read-str)
            (assoc "files" files)
            (assoc "success?" true)
            (dissoc "className" "createdAt" "objectId" "updatedAt" "user")))
      (catch NullPointerException e
        {}))))

(defn get-base-info
  []
  (wrap-try
    (if-let [user (AVUser/getCurrentUser)]
      (-> (.fetch user)
          (.toJSONObject)
          (.toString)
          (read-str)
          (dissoc "className" "emailVerified" "createdAt" "sessionToken" "objectId" "email" "username" "updatedAt" "webpointer")
          (assoc "success?" true))
      {:success? false})))

(defn save-file
  [file]
  (wrap-try
    (let [name (:filename file)
          file ^AVFile (save-file-bytes name (:bytes file))
          user (AVUser/getCurrentUser)
          info (-> (avos-query "SignUp")
                   (query-object ["user" := user])
                   (query-first))
          info (if (nil? info) (-> (avos-object "SignUp")
                                   (put-object ["user" user])
                                   (save-object))
                               info)]
      (.add info "files" file)
      (save-object info)
      {:success? true
       :name     name
       :id       (.getObjectId file)
       :url      (.getUrl file)})))

(defn delete-file
  [^String id]
  (wrap-try
    (let [user ^AVObject (-> (avos-query "SignUp")
                             (query-object ["user" := (AVUser/getCurrentUser)])
                             (query-first))
          file ^AVFile (-> (avos-query "_File")
                           (.get id))]
      (.removeAll user "files" [file])
      (save-object user)
      {:success? true})))
