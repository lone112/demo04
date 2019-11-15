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

(defn auth-request [act path & rs]
  (my-app (-> (mock/request act path)
              (mock/header "Authorization" (str "Token " token))
              (mock/json-body (first rs)))))

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
                  (demo04.handler-tag/get-database anything) => nil :times any?)))

(fact "api/options"
      (let [fake-cities {:id "123456" :name "layer01" :cities [{:id "654321" :name "layer02"}]}
            fake-brands [{:id "2345" :name "b2345"} {:id "2346" :name "b2346"}]
            fake-products [{:id "2345" :name "p2345"} {:id "2346" :name "p2346"}]
            expect-str (json/generate-string {:districts fake-cities
                                              :brands    fake-brands
                                              :products  fake-products})
            resp (delay (auth-request :get "/api/options"))]
        (:body @resp) => (every-checker (contains "id") (contains expect-str))
        (provided (monger.collection/aggregate anything "cities" anything) => fake-cities
                  (monger.collection/aggregate anything "brands" anything) => fake-brands
                  (monger.collection/aggregate anything "products" anything) => fake-products
                  (demo04.handler-tag/get-database anything) => nil :times any?)))

(fact "/api/profiles/tags"
      (let [fake-tags [{:id "123" :name "n123"}]
            expect-str (json/generate-string fake-tags)
            resp (delay (auth-request :get "/api/profiles/tags"))]
        (:body @resp) => (contains expect-str)
        (provided (monger.collection/aggregate anything "tags" anything) => fake-tags
                  (demo04.handler-tag/get-database anything) => nil :times any?)))

(fact "PUT /api/profiles/groups"
      (let [fake-group {:id "5db273a7167610078c7435a5" :name "hello"}
            resp (delay (auth-request :put (str "/api/profiles/groups/" (:id fake-group))
                                      fake-group))]
        (:status @resp) => 200
        (provided (monger.collection/update-by-id anything "groups" anything anything) => nil
                  (demo04.handler-tag/get-database anything) => nil :times any?)))

(fact "POST /api/profiles/groups"
      (let [fake-group {:name "hello" :id "5db273a7167610078c7435a5"}
            post-data {:name "hello"}
            resp (delay (auth-request :post "/api/profiles/groups" post-data))]
        (:body @resp) => (contains (json/generate-string fake-group))
        (provided (monger.collection/insert-and-return anything "groups" anything) => fake-group
                  (demo04.handler-tag/get-database anything) => nil :times any?)))

(fact "/api/profiles/groups/:id"
      (let [fake-group {:name "hello" :id "5db273a7167610078c7435a5"}
            resp (delay (auth-request :get (str "/api/profiles/groups/" (:id fake-group))))]
        (:body @resp) => (contains (json/generate-string fake-group))
        (provided (monger.collection/find-map-by-id anything "groups" anything) => fake-group
                  (demo04.handler-tag/get-database anything) => nil :times any?)))

(fact "/api/profiles/groups"
      (let [fake-groups [{:name "hello" :id "5db273a7167610078c7435a5"}
                         {:name "hello1" :id "5db273a7167610078c7435a6"}]
            resp (delay (auth-request :get "/api/profiles/groups"))]
        (:body @resp) => (every-checker (contains (:id (first fake-groups))) (contains "count") (contains "["))
        (provided (monger.collection/find-maps anything "groups") => fake-groups
                  (demo04.handler-tag/get-database anything) => nil :times any?)))