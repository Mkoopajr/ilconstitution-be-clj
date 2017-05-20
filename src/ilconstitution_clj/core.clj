(ns ilconstitution-clj.core
  (:require [clojure.data.json :as json]
            [clojure.string :as str]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.adapter.jetty :as ring]
            [ring.middleware.session :as session]
            [ring.middleware.params :as params]
            [ring.middleware.multipart-params :as multiparams]
            [ring.middleware.json :refer [wrap-json-params]]
            [taoensso.carmine :as car :refer (wcar)])
  (:gen-class))

(def conn-opt {:pool {} :spec {:host "localhost" :port 6379}})
(defmacro wcar* [& body] `(car/wcar conn-opt ~@body))

(defn setSession [session headers]
  (if (:id session)
    (do
      (println "test already started")
      {:status 302
       :headers {"Location" "/question",
                 "Access-Control-Allow-Origin" "http://www.ilconstitution.com",
                 "Access-Control-Allow-Methods" "POST,GET,OPTIONS",
                 "Access-Control-Allow-Headers" "Content-Type",
                 "Access-Control-Allow-Credentials" "true"}
       :body ""})
    (do
      (println "starting test")
      (def id (str (java.util.UUID/randomUUID)))
      (wcar* (car/sunionstore (str id ":keys") "q:keys"))
      (wcar* (car/expire (str id ":keys") 60))
      (println (str id ":keys"))
      {:status 302 
       :headers {"Location" "/question",
                 "Access-Control-Allow-Origin" "http://www.ilconstitution.com",
                 "Access-Control-Allow-Methods" "POST, GET, OPTIONS",
                 "Access-Control-Allow-Headers" "Content-Type",
                 "Access-Control-Allow-Credentials" "true"}
       :body ""
       :session (assoc session :id id)})))

(defn getQuestion [session]
  (if-not (:id session)
    {:status 302
     :headers {"Location" "/start",
               "Access-Control-Allow-Origin" "http://www.ilconstitution.com",
                 "Access-Control-Allow-Methods" "POST, GET, OPTIONS",
                 "Access-Control-Allow-Headers" "Content-Type",
                 "Access-Control-Allow-Credentials" "true"}
     :body ""}
    (do
      (def rkey (wcar* (car/spop (str (:id session) ":keys"))))
      (def rdata (wcar* (car/get rkey)))
      {:status 200
       :headers {"Access-Control-Allow-Origin" "http://www.ilconstitution.com",
                 "Access-Control-Allow-Methods" "POST, GET, OPTIONS",
                 "Access-Control-Allow-Headers" "Content-Type",
                 "Access-Control-Allow-Credentials" "true"}
       :body rdata})))

(defn checkAnswer [session params]
  (if-not (:id session)
    {:status 302
     :headers {"Location" "/start",
               "Access-Control-Allow-Origin" "http://www.ilconstitution.com",
                 "Access-Control-Allow-Methods" "POST, GET, OPTIONS",
                 "Access-Control-Allow-Headers" "Content-Type",
                 "Access-Control-Allow-Credentials" "true"}
     :body ""}
    (do
      (println params)
      {:status 200
       :headers {"Access-Control-Allow-Origin" "http://www.ilconstitution.com",
                 "Access-Control-Allow-Methods" "POST, GET, OPTIONS",
                 "Access-Control-Allow-Headers" "Content-Type",
                 "Access-Control-Allow-Credentials" "true"}
       :body "checking..."})))

(defroutes app-routes
  (GET "/start" {session :session, headers :headers} (setSession session headers))
  (GET "/question" {session :session} (getQuestion session))
  ;(POST "/check" {session :session params :multipart-params} (checkAnswer session params))
  (POST "/check" request
   (println request)
   {:status 200
       :headers {"Access-Control-Allow-Origin" "http://www.ilconstitution.com",
                 "Access-Control-Allow-Methods" "POST, GET, OPTIONS",
                 "Access-Control-Allow-Headers" "Content-Type",
                 "Access-Control-Allow-Credentials" "true"}
       :body (str request)})
  (OPTIONS "/check" request
   {:status 200
       :headers {"Access-Control-Allow-Origin" "http://www.ilconstitution.com",
                 "Access-Control-Allow-Methods" "POST, GET, OPTIONS",
                 "Access-Control-Allow-Headers" "Content-Type",
                 "Access-Control-Allow-Credentials" "true"}
       :body (str request)})
  (route/not-found "Error 404"))

(def app
  (-> app-routes
      (session/wrap-session {:cookie-attrs {:max-age 60}})
      ;(params/wrap-params)
      ;(multiparams/wrap-multipart-params)
      (wrap-json-params)))

(defn -main []
  (ring/run-jetty #'app {:port 8080 :join? false}))
