(ns core.route
  (:use [ring.util.servlet :only (defservice)]
        compojure.core
        compojure.route
        core.sign-up
        core.util
        ring.middleware.json
        ring.middleware.keyword-params
        [ring.util.response :exclude (not-found)]
        clojure.tools.logging)
  (:import [com.avos.avoscloud AVUser])
  (:gen-class
    :extends javax.servlet.http.HttpServlet))

(def wrap-json-params-routes
  (comp wrap-json-response
        wrap-json-params
        wrap-keyword-params))

(defroutes
  sign-up-routes*
  (-> (POST "/sign-up" [id name phone]
        (response (sign-up id name phone)))
      (wrap-routes wrap-json-params-routes))

  (-> (POST "/verify-phone" [code]
          (response (verify-phone code)))
      (wrap-routes wrap-json-params-routes))

  (-> (POST "/request-phone-verify" [phone]
        (response (request-phone-verify phone)))
      (wrap-routes wrap-json-params-routes))

  (GET "/" [] (resource-response "index.html")))

(defservice (routes sign-up-routes*
                    (not-found "404 Not Found")))