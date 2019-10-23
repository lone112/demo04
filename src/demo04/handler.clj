(ns demo04.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults api-defaults]]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
            [ring.util.response :refer [response]]
            [demo04.handler-tag :as tag]))

(defn handler [request]
  (response {:foo "bar"}))

(defroutes app-routes
           (GET "/" [] "Hello World")
           (GET "/test" [] handler)
           (GET "/users" [] tag/handler)
           (GET "/tag" [] tag/all-tag)
           (POST "/tag" [] tag/new-tag)
           (PUT "/tag/:id" [] tag/update-tag)
           (POST "/users/apply" [] tag/batch-update)
           (route/not-found "Not Found"))

(def app
  (wrap-defaults (-> app-routes
                     (wrap-json-body {:keywords? true :bigdecimals? true})
                     wrap-json-response) api-defaults))
