(ns core.init
  (:gen-class :implements [javax.servlet.ServletContextListener])
  (:import (com.avos.avoscloud AVOSCloud)
           (cn.leancloud LeanEngine EngineSessionCookie)))

(defn -contextInitialized
  [this arg1]
  (LeanEngine/initialize
    (System/getenv "LEANCLOUD_APP_ID")
    (System/getenv "LEANCLOUD_APP_KEY")
    (System/getenv "LEANCLOUD_APP_MASTER_KEY"))
  (LeanEngine/setHttpsRedirectEnabled true))

(defn -contextDestroyed [this arg1])