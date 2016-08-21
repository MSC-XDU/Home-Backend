(ns core.route
  (:use [ring.util.servlet :only (defservice)]
        compojure.core
        core.sign-up
        core.util
        ring.middleware.json
        ring.middleware.keyword-params
        ring.middleware.multipart-params
        ring.middleware.multipart-params.byte-array
        ring.util.response
        clojure.tools.logging)
  (:import [com.avos.avoscloud AVUser])
  (:gen-class
    :extends javax.servlet.http.HttpServlet))

(defn- wrap-check-login
  [handler]
  (fn [request]
    (if-let [user (AVUser/getCurrentUser)]
      (handler request)
      (-> (response "error")
          (status 401)))))

(def wrap-json-params-routes
  (comp wrap-json-response
        wrap-json-params
        wrap-keyword-params))

(def wrap-json-body-routes
  (comp wrap-json-response
        wrap-json-body))

(def ^:private allowed-origin
  #{"https://www.xdmsc.club"
    "http://www.xdmsc.club"
    "https://xdmsc.club"
    "http://xdmsc.club"
    "http://127.0.0.1:8080"})

(def ^:private allowed-method #{"GET" "POST"})

(def ^:private allowed-header #{"Content-Type"})

(defroutes
  sign-up-routes*
  (context "/join" []
    (-> (POST "/sign-up" [username password]
          (response (sign-up username password)))
        (wrap-routes wrap-json-params-routes))

    (-> (POST "/verify-phone" [code]
          (response (verify-phone code)))
        (wrap-routes wrap-json-params-routes))

    (-> (POST "/request-verify" [phone]
          (response (request-phone-verify phone)))
        (wrap-routes (comp wrap-check-login
                           wrap-json-params-routes)))

    (-> (POST "/set-base-info" [:as {body :body}]
          (response (set-base-info body)))
        (wrap-routes (comp wrap-check-login
                           wrap-json-body-routes)))

    (-> (POST "/set-sign-up-info" [:as {body :body}]
          (response (set-sign-up-info body)))
        (wrap-routes (comp wrap-check-login
                           wrap-json-body-routes)))

    (-> (POST "/user-exist" [email :as req]
          (response (user-exist? email)))
        (wrap-routes wrap-json-params-routes))

    (-> (POST "/login" [email password]
          (response (log-in email password)))
        (wrap-routes wrap-json-params-routes))

    (-> (GET "/sign-up-info" []
          (response (get-sign-up-info)))
        (wrap-routes (comp wrap-check-login
                           wrap-json-response)))

    (-> (GET "/base-info" []
          (response (get-base-info)))
        (wrap-routes (comp wrap-check-login
                           wrap-json-response)))

    (-> (POST "/file" [file :as r]
          (response (save-file file)))
        (wrap-routes (comp wrap-check-login
                           wrap-json-response
                           #(wrap-multipart-params % {:store (byte-array-store)}))))

    (-> (POST "/delete-file" [id]
          (response (delete-file id)))
        (wrap-routes wrap-json-params-routes))

    (OPTIONS "*" []
      (response ""))))

(def sign-up-routes
  (wrap-cors sign-up-routes* allowed-origin allowed-method allowed-header))

(defservice sign-up-routes)