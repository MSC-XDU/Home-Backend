(ns leanengine.object
  "对 leancloud 云引擎的简单封装"
  (:import (com.avos.avoscloud AVObject AVSaveOption AVQuery AVRelation)
           (java.lang IllegalArgumentException)
           (java.text SimpleDateFormat))
  (:use [leanengine.base])
  (:require [clojure.data.json :as json]))

(defn- handle-value
  "将数据中所有的 sym 和 keyword 转化成 string
   转化后除了 map 之外的 coll 均变为 seq"
  [v]
  (cond
    (instance? AVObject v) v
    (or (keyword? v) (symbol? v)) (name v)
    (string? v) v
    (map? v) (apply hash-map (mapcat handle-value v))
    (coll? v) (map handle-value v)
    :else v))

(defn- save-map-flatten
  "将 map 展开为 key-value 对 存储在 leancloud 中"
  [^AVObject avos-object map]
  (let [seq (seq map)]
    (avos-try
      (doseq [[k v] seq]
        (.put avos-object (handle-value k) (handle-value v)))
      avos-object)))

(defn- save-key-value
  "将 value 存入 avos-object 的 key 中
   支持 map list set string vec 数据
   其中的 sym 和 keyword 将转化成 string
   keyword 丢弃 namespace"
  [^AVObject avos-object ^String k value]
  (avos-try
    (doto avos-object
      (.put k (handle-value value)))))

(defn- put-kv
  [avos-object k v]
  (cond
    (= :flatten-object k)
    (save-map-flatten avos-object v)
    :else
    (save-key-value avos-object k v)))

(defn avos-object
  ([^String class-name]
   ^AVObject (AVObject. class-name))
  ([^String class-name ^String id]
   ^AVObject (AVObject/createWithoutData class-name id)))

(defn put-object
  "提供 avos-object [bidings]
   bindings 形式为 [key1 value1 key2 value2] 或者是 一个 map
   key 需为 string
   value 可为 set map list set string vec sym keyword
   value 中所有的 sym keyword 均化为 string
   除了 map 之外的 coll 均变为 seq
   如 key 为 :flatten-object value 为 map 则将 map 的 key value 展开存入 avos-object (只展开一层)"
  [^AVObject avos-object bindings]
  (if (map? bindings)
    (save-map-flatten avos-object bindings)
    (loop [bindings bindings]
      (if-let [seq (seq bindings)]
        (let [[^String k v] seq]
          (cond
            (not-any? nil? [k v])
            (do
              (put-kv avos-object k v)
              (recur (drop 2 seq)))
            :else
            (throw (IllegalArgumentException. "保存DSL绑定错误!"))))
        avos-object))))

(defn inc-field
  ([^AVObject object ^String field]
   (inc-field object field 1))
  ([^AVObject object ^String field ^Number num]
   (.increment object field num)
   object))

(defn dec-field
  ([^AVObject object ^String field]
   (dec-field object field 1))
  ([^AVObject object ^String field ^Number num]
   (.increment object field (- num))
   object))

(defn add-relation
  [^AVObject parent ^String name & objects]
  (let [^AVRelation relation (.getRelation parent name)]
    (doseq [^AVObject object objects] (.add relation object)))
  parent)

(defn remove-relation
  [^AVObject parent ^String name & objects]
  (let [^AVRelation relation (.getRelation parent name)]
    (doseq [^AVObject object objects] (.remove relation object)))
  parent)

(defn array-add
  [^AVObject obj ^String field object]
  (.addUnique obj field object)
  obj)

(defn save-object
  ([^AVObject object]
   (doto object (.save)))
  ([^AVObject object options]
   (if-let [options (apply hash-map options)]
     (do
       (when-let [{fetch? :fetch} options]
         (.setFetchWhenSave object fetch?))
       (if-let [{^AVQuery query :query} options]
         (doto object (.save (.query (AVSaveOption.) query)))
         (doto object (.save)))
       ))))

(defn save-objects
  [objs]
  (AVObject/saveAll objs)
  objs)

(defn get-slot
  "获取 leancloud 数据行中的数据"
  [^AVObject object bindings]
  (loop [bindings bindings result (transient {})]
    (if-let [seq (seq bindings)]
      (let [[k type] seq]
        (cond
          (= :str type)
          (recur (drop 2 seq) (assoc! result (keyword k) (.getString object k)))
          (= :num type)
          (recur (drop 2 seq) (assoc! result (keyword k) (.getInt object k)))
          (= :bool type)
          (recur (drop 2 seq) (assoc! result (keyword k) (.getBoolean object k)))
          (= :map type)
          (recur (drop 2 seq) (assoc! result (keyword k) (json/read-str (.toString (.getJSONObject object k)))))
          (= :seq type)
          (recur (drop 2 seq) (assoc! result (keyword k) (json/read-str (.toString (.getJSONArray object k)))))
          (= :object-id type)
          (recur (drop 2 seq) (assoc! result (keyword k) (.getObjectId object)))
          (= :date type)
          (recur (drop 2 seq) (assoc! result (keyword k) (.format (SimpleDateFormat. "yyyy-MM-dd HH:mm:ss") (.getCreatedAt object))))))
      (persistent! result))))