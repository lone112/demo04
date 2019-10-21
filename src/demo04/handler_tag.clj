(ns demo04.handler-tag
  (:require [monger.core :as mg]
            [monger.collection :as mc]
            [monger.conversion :refer [to-db-object from-db-object]]
            [ring.util.response :refer [response bad-request]])
  (:import
    org.bson.types.ObjectId
    (com.mongodb DuplicateKeyException)))

(defn convert-to-map [obj]
  (let [m (from-db-object obj true)]
    (->
      (dissoc m :_id)
      (assoc :id (str (:_id m)))
      )))

(defn handler [request]
  (let [conn (mg/connect {:host "10.126.21.136"})
        db (mg/get-db conn "demo04")
        coll "user_profile"
        cr (mc/find db coll)]
    (response (mapv convert-to-map cr))
    ))


(defn new-tag [request]
  (let [conn (mg/connect {:host "10.126.21.136"})
        db (mg/get-db conn "demo04")
        coll "tags"]
    (try
      (prn (:body request))
      (response (convert-to-map (mc/insert-and-return db coll {:name (get-in request [:body :name])})))
      (catch DuplicateKeyException e
        (bad-request (.getMessage e))
        ))
    ))



(defn all-tag [request]
  (let [conn (mg/connect {:host "10.126.21.136"})
        db (mg/get-db conn "demo04")
        coll "tags"
        cr (mc/find db coll)]
    (response (mapv convert-to-map cr)))
  )

(defn update-tag [request]
  (let [conn (mg/connect {:host "10.126.21.136"})
        db (mg/get-db conn "demo04")
        coll "tags"
        id (get-in request [:params :id])
        name (get-in request [:body :name])]
    (try
      (mc/update-by-id db coll (ObjectId. id) {:name name})
      (response "OK")
      (catch DuplicateKeyException e
        (bad-request (.getMessage e))
        )))
  )