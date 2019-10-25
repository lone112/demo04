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



(defn all-group [request]
  (let [db (mg/get-db conn "demo04")
        coll "groups"]
    (response (mapv convert-to-map (mc/find db coll)))
    ))


(defn new-group [request]
  (let [db (mg/get-db conn "demo04")
        coll "groups"]
    (try
      (prn (:body request))
      (response (convert-to-map (mc/insert-and-return db coll {:name (get-in request [:body :name])
                                                               :tags (get-in request [:body :items])})))
      (catch DuplicateKeyException e
        (bad-request (.getMessage e))
        ))
    ))

(defn- try-parse-oid [s]
  (try
    (ObjectId. s)
    (catch Exception e
      nil
      )))


(defn del-group [request]
  (let [db (mg/get-db conn "demo04")
        coll "groups"]
    (if-let [oid (try-parse-oid (get-in request [:params :id]))]
      (response {:status "OK"
                 :msg    (.toString (mc/remove-by-id db coll oid))})
      (response {:status "OK"}))
    ))

(defn- split-string [s]
  (->> (clojure.string/split s #",")
       (map clojure.string/trim)
       (filter #(< 1 (count %)))
       ))

(defn- get-group-tags [id]
  (prn (try-parse-oid id))
  (if-let [oid (try-parse-oid id)]
    (-> (mc/find-map-by-id (mg/get-db conn "demo04") "groups" oid)
        (:tags))))

(defn tag-vec [request]
  (let [m (:params request)]
    (cond
      (:t m) (split-string (:t m))
      (:g m) (get-group-tags (:g m)))))

(defn query [request]
  (let [db (mg/get-db conn "demo04")
        coll "user_profile"]
    (if-let [ids (tag-vec request)]
      (response {:count (mc/count db coll {:tags {$all ids}})
                 :items (->>
                          (with-collection db coll
                                           (find {:tags {$all ids}})
                                           (limit 50))
                          (mapv convert-to-map))})
      (response []))))