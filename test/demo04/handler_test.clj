(ns demo04.handler-test
  (:require [clojure.test :refer :all]
            [ring.mock.request :as mock]
            [clojure.string :as string]
            [demo04.handler :refer :all :rename {app my-app}]))

(deftest test-app
  (testing "main route"
    (let [response (my-app (mock/request :get "/"))]
      (is (= (:status response) 200))
      (is (= (:body response) "Hello World"))))

  (testing "not-found route"
    (let [response (my-app (mock/request :get "/invalid"))]
      (is (= (:status response) 404))))

  (testing "login 401"
    (let [response (my-app (-> (mock/request :post "/api/account/signin")
                               (mock/json-body {:username "hello" :password "world"})))]
      (is (= 401 (:status response)))))

  (testing "login 200"
    (let [response (my-app (-> (mock/request :post "/api/account/signin")
                               (mock/json-body {:username "admin" :password "secret"})))]
      (is (= 200 (:status response)))
      (is (string/includes? (:body response) "token")))))
