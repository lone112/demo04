(ns demo04.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults api-defaults]]
            [ring.middleware.json :refer [wrap-json-response]]
            [ring.util.response :refer [response]]))

(defn handler [request]
  (response {:foo "bar"}))

(defroutes app-routes
           (GET "/" [] "Hello World")
           (GET "/test" [] handler)
           (route/not-found "Not Found"))

(def app
  (wrap-defaults (wrap-json-response app-routes) api-defaults))
