(ns leanengine.query
  (:import (com.avos.avoscloud AVQuery AVObject)
           (java.lang IllegalArgumentException))
  (:use leanengine.base))

(defn avos-query
  [^String class-name]
  (AVQuery. class-name))

;生成代码的宏
;(def ^:private rel-bindings
;  {
;   :=      '.whereEqualTo
;   :not=   '.whereNotEqualTo
;   :>      '.whereGreaterThan
;   :>=     '.whereGreaterThanOrEqualTo
;   :<      '.whereLessThan
;   :<=     '.whereLessThanOrEqualTo
;   :regexp '.whereMatches
;   :%      '.whereContains
;   :not%   '.whereNotContainedIn
;   :$      '.whereStartsWith
;   })
;
;(defmacro ^:private avos-rels
;  [q f r c]
;  `(cond
;     ~@(mapcat
;         identity
;         (for [rel (keys rel-bindings)]
;           [`(= ~rel ~r) `(~(get rel-bindings rel) ~q ~f ~c)]))
;     :else
;     (throw (IllegalArgumentException. "保存DSL绑定错误!"))))

(defn query-object
  "leancloud 查询接口"
  [^AVQuery avos-query bindings]
  (loop [bindings bindings]
    (if-let [seq (seq bindings)]
      (let [[^String field relation condition] seq]
        (do
          ;因 Java 方法调用的问题,暂时用宏展开的代码处理
          ;todo 反射调用
          (clojure.core/cond
            (clojure.core/= :<= relation)
            (.whereLessThanOrEqualTo avos-query field condition)
            (clojure.core/= :not= relation)
            (.whereNotEqualTo avos-query field condition)
            (clojure.core/= :> relation)
            (.whereGreaterThan avos-query field condition)
            (clojure.core/= :% relation)
            (.whereContains avos-query field condition)
            (clojure.core/= :$ relation)
            (.whereStartsWith avos-query field condition)
            (clojure.core/= :>= relation)
            (.whereGreaterThanOrEqualTo avos-query field condition)
            (clojure.core/= :not% relation)
            (.whereNotContainedIn avos-query field condition)
            (clojure.core/= :regexp relation)
            (.whereMatches avos-query field condition)
            (clojure.core/= := relation)
            (.whereEqualTo avos-query field condition)
            (clojure.core/= :< relation)
            (.whereLessThan avos-query field condition)
            :else (throw (IllegalArgumentException. "保存DSL绑定错误!")))
          (recur (drop 3 seq))))
      avos-query)))

(defn query-or
  "leancloud 组合 or 查询"
  [& queries]
  ^AVQuery (AVQuery/or queries))

(defn query-and
  "leancloud 组合 and 查询"
  [& queries]
  ^AVQuery (AVQuery/and queries))

(defn query-first
  [^AVQuery query]
  (avos-try
    (.getFirst query)))

(defn query-limit
  [^AVQuery query limit]
  ^AVQuery (.limit query limit))

(defn query-skip
  [^AVQuery query skip]
  ^AVQuery (.skip query skip))

(defn only-keys
  [^AVQuery query keys]
  ^AVQuery (.selectKeys query keys))

(defn query-count
  [^AVQuery query]
  (.count query))

(defn query-find
  [^AVQuery query]
  (avos-try
    (.find query)))

(defn query-order
  [^AVQuery query bindings]
  (loop [bindings bindings]
    (if-let [seq (seq bindings)]
      (let [[order field] seq]
        (do
          (cond
            (= :desc order)
            (.addDescendingOrder query field)
            (= :asc order)
            (.addAscendingOrder query field))
          (recur (drop 2 seq))))
      query)))

(defn relation-query
  [^AVObject object ^String rel]
  (.getQuery (.getRelation object rel)))

(defn query-include
  [^AVQuery query ^String include]
  (.include query include))