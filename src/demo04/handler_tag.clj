(ns demo04.handler-tag
  (:require [monger.core :as mg]
            [monger.collection :as mc]
            [monger.query :refer :all]
            [monger.operators :refer [$each $addToSet $pull $in $all]]
            [monger.conversion :refer [to-db-object from-db-object]]
            [ring.util.response :refer [response bad-request]])
  (:import (com.mongodb DuplicateKeyException)
           (org.bson.types ObjectId)))

(def ^:private conn (mg/connect {:host "10.126.21.136"}))

(defn convert-to-map [obj]
  (let [m (from-db-object obj true)]
    (->
      (dissoc m :_id)
      (assoc :id (str (:_id m)))
      )))

(defn handler [request]
  (let [db (mg/get-db conn "demo04")
        coll "user_profile"
        cr (mc/find db coll)]
    (response (mapv convert-to-map cr))
    ))


(defn new-tag [request]
  (let [db (mg/get-db conn "demo04")
        coll "tags"]
    (try
      (prn (:body request))
      (response (convert-to-map (mc/insert-and-return db coll {:name (get-in request [:body :name])})))
      (catch DuplicateKeyException e
        (bad-request (.getMessage e))
        ))
    ))





(defn all-tag [request]
  (let [db (mg/get-db conn "demo04")
        coll "tags"]
    (response (mapv convert-to-map (mc/find db coll)))
    )
  )

(defn update-tag [request]
  (let [db (mg/get-db conn "demo04")
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



(defn batch-update [request]
  (let [db (mg/get-db conn "demo04")
        coll "user_profile"]
    (prn (:body request))
    (doseq [{:keys [id tags del]} (:body request)]
      (if (seq tags)
        (mc/update-by-id db coll (ObjectId. id) {$addToSet {:tags {$each tags}}}))
      (if (seq del)
        (mc/update-by-id db coll (ObjectId. id) {$pull {:tags {$in del}}}))
      )
    (response "OK")
    )
  )


(defn query [request]
  (let [db (mg/get-db conn "demo04")
        coll "user_profile"
        t (get-in request [:params :t] "")
        strs (->> (clojure.string/split t #",")
                  (map clojure.string/trim)
                  (filter #(< 1 (count %)))
                  )]
    (if (seq strs)
      (response {:count (mc/count db coll {:tags {$all strs}})
                 :items (->>
                          (with-collection db coll
                                           (find {:tags {$all strs}})
                                           (limit 50))
                          (mapv convert-to-map))})
      (response []))))
