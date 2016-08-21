(ns core.util
  (:use ring.util.response))

(defn set-cors
  [response origin allowed-method allowed-header]
  (when response
    (-> (header response "Access-Control-Allow-Origin" origin)
        (header "Access-Control-Allow-Methods" (apply str (interpose "," allowed-method)))
        (header "Access-Control-Allow-Credentials" true)
        (header "Access-Control-Allow-Headers" (apply str (interpose "," allowed-header)))
        (header "Access-Control-Max-Age" 1728000))))

(defn wrap-cors
  [handler allowed-origin allowed-method allowed-header]
  (fn [request]
    (let [origin (get-header request "origin")
          req-method (get-header request "access-control-request-method")
          req-header (get-header request "access-control-request-headers")
          resp (handler request)]
      (if (and (allowed-origin origin) (or (nil? req-method) (allowed-method req-method)))
        (set-cors resp origin allowed-method allowed-header)))))

(defn get-cors-wraper
  [allowed-origin allowed-method allowed-header]
  #(wrap-cors % allowed-origin allowed-method allowed-header))