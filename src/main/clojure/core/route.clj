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
    (if-let [token (get (:params request) :token (get (:params request) "token"))]
      (if-let [user (AVUser/becomeWithSessionToken token)]
        (handler (assoc-in request [:params :user] user))
        (-> (response "error")
            (status 401)))
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
        (wrap-routes (comp wrap-json-params-routes)))

    (-> (POST "/set-base-info" [user data]
          (response (set-base-info data user)))
        (wrap-routes (comp wrap-json-params-routes
                           wrap-check-login)))

    (-> (POST "/set-sign-up-info" [user data]
          (response (set-sign-up-info data user)))
        (wrap-routes (comp wrap-json-params-routes
                           wrap-check-login)))

    (-> (POST "/user-exist" [email]
          (response (user-exist? email)))
        (wrap-routes wrap-json-params-routes))

    (-> (POST "/login" [email password]
          (response (log-in email password)))
        (wrap-routes wrap-json-params-routes))

    (-> (POST "/sign-up-info" [user]
          (response (get-sign-up-info user)))
        (wrap-routes (comp wrap-json-params-routes
                           wrap-check-login)))

    (-> (POST "/base-info" [user]
          (response (get-base-info user)))
        (wrap-routes (comp wrap-json-params-routes
                           wrap-check-login)))

    (-> (POST "/file" [file user :as req]
          (response (save-file file user)))
        (wrap-routes (comp wrap-json-response
                           #(wrap-multipart-params % {:store (byte-array-store)})
                           wrap-check-login)))

    (-> (POST "/delete-file" [id user]
          (response (delete-file id user)))
        (wrap-routes (comp wrap-json-params-routes
                           wrap-check-login)))

    (OPTIONS "*" []
      (response ""))))

(def sign-up-routes
  (wrap-cors sign-up-routes* allowed-origin allowed-method allowed-header))

(defservice (routes
              sign-up-routes
              (ANY "*" [] (-> (response "notfound")
                              (status 404)))))