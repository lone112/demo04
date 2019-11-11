(ns demo04.tag-test
  (:use midje.sweet)
  (:require [clojure.test :refer :all]
            [midje.sweet :refer :all]
            [ring.mock.request :as mock]
            [clojure.string :as str]
            [demo04.handler :refer :all :rename {app my-app}]
            [buddy.sign.jwt :as jwt]
            [clj-time.core :as time]
            [cheshire.core :as json]
            [cheshire.generate :refer [add-encoder encode-str]]
            [monger.collection :as mc])
  (:import (org.bson.types ObjectId)))

(add-encoder ObjectId encode-str)

(defn- gen-token []
  (let [claims {:user (keyword "admin")
                :exp  (time/plus (time/now) (time/seconds 3600))}]
    (jwt/sign claims secret {:alg :hs512})))

(def token (gen-token))

(defn auth-request [act path]
  (my-app (-> (mock/request act path)
              (mock/header "Authorization" (str "Token " token)))))

(facts "about `split`"
       (str/split "a/b/c" #"/") => ["a" "b" "c"]
       (str/split "" #"irrelvant") => [""]
       (str/split "no regexp matches" #"a+\s+[ab]") => ["no regexp matches"])

(fact "Home"
      (let [response (auth-request :get "/")]
        (:status response) => 200
        (:body response) => "Hello World"))

(fact "User Profile"
      (let [response (auth-request :get "/api/account/userprofile")]
        (:status response) => 200
        (:body response) => (contains "id")))

(fact "Main info"
      (let [uid (ObjectId.)
            fake-user {:id          uid
                       :displayName "Hello"
                       :sex         "F"
                       :birthday    "2009-12-12"
                       :tags        ["金牌会员" "高消费" "参与双十一" "活跃用户"]
                       :phone       "13964578901"
                       :email       "hello@world.com"
                       :address     "china shanghai"}
            resp (fn [] (auth-request :get (str "/api/customer/maininfo?id=" (str uid))))]
        (:body (resp)) => (contains (json/generate-string fake-user))
        (provided (monger.collection/find-map-by-id anything anything anything anything) => fake-user
                  (monger.core/get-db anything anything) => nil
                  (demo04.handler-tag/init-conn) => nil :times any?)))
