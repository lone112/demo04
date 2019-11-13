(ns demo04.handler
  (:require [clojure.tools.logging :as log]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults api-defaults]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
            [ring.middleware.cors :refer [wrap-cors]]
            [ring.util.response :refer [response]]
            [demo04.handler-tag :as tag]
            [ring.util.response :refer [response redirect content-type]]
            [ring.adapter.jetty :as jetty]
            [clj-time.core :as time]
            [buddy.sign.jwt :as jwt]

            [buddy.auth.backends.token :refer [jws-backend]]
            [buddy.auth :refer [authenticated? throw-unauthorized]]
            [buddy.auth.backends.httpbasic :refer [http-basic-backend]]
            [buddy.auth.middleware :refer [wrap-authentication wrap-authorization]])
  (:gen-class))

(defn test-handler [request]
  (if (authenticated? request)
    (response "Hello")
    (response "401")))

(defn ok [d] {:status 200 :body d})
(defn bad-request [d] {:status 400 :body d})

(def secret "mysupersecret")

;; Global var that stores valid users with their
;; respective passwords.

(def authdata {:admin "secret"
               :test  "secret"})

;; Create an instance of auth backend.
(def auth-backend (jws-backend {:secret secret :options {:alg :hs512}}))

;; Authenticate Handler
;; Responds to post requests in same url as login and is responsible for
;; identifying the incoming credentials and setting the appropriate authenticated
;; user into session. `authdata` will be used as source of valid users.

(defn login
  [request]
  (let [username (get-in request [:body :username])
        password (get-in request [:body :password])
        valid? (some-> authdata
                       (get (keyword username))
                       (= password))]
    (if valid?
      (let [claims {:user (keyword username)
                    :exp  (time/plus (time/now) (time/seconds 3600))}
            token (jwt/sign claims secret {:alg :hs512})]
        (ok {:token       token
             :id          "ac0001"
             :displayName "Admin"}))
      {:message "wrong auth data"
       :status  401})))

(defn wrap-require-auth [handler]
  (fn [req]
    (if (or (System/getenv "DEBUG") (authenticated? req))
      (handler req)
      {:status 401})))

(def api-routes
  (routes (GET "/customer/list" [] tag/user-list)
          (GET "/customer/maininfo" [] tag/user-info)
          (GET "/customer/scoreinfo" [] tag/user-score)
          (GET "/customer/preferenceinfo" [] tag/user-prefer)
          (GET "/customer/activity" [] tag/user-activity)
          (GET "/profiles/tags" [] tag/all-tag)
          (POST "/tag" [] tag/new-tag)
          (PUT "/tag/:id" [] tag/update-tag)
          (GET "/profiles/groups" [] tag/all-group)
          (GET "/profiles/groups/:id" [] tag/all-group)
          (POST "/profiles/groups" [] tag/new-group)
          (DELETE "/profiles/groups" [] tag/del-group)
          (POST "/users/apply" [] tag/batch-update)
          (GET "/users/query" [] tag/query)
          (GET "/options" [] tag/cities)
          (GET "/account/userprofile" [] tag/user-profile)))

(defroutes
  ring-routes
  (GET "/" [] "Hello World")
  (GET "/test" [] test-handler)
  (POST "/api/account/signin" [] login)
  (wrap-routes (context "/api" [] api-routes) wrap-require-auth)
  (route/not-found "Not Found"))

(def app
  (-> ring-routes
      (wrap-keyword-params)
      (wrap-params)
      (wrap-authorization auth-backend)
      (wrap-authentication auth-backend)
      (wrap-json-response {:pretty false})
      (wrap-json-body {:keywords? true :bigdecimals? true})
      (wrap-cors :access-control-allow-origin [#".*"] :access-control-allow-methods [:get :post]
                 :access-control-allow-credentials "true")))

(defn -main
  [& args]
  (log/info "app start")
  (jetty/run-jetty app {:port 3000}))
