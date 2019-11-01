(ns demo04.handler-tag
  (:refer-clojure :exclude [sort find])
  (:require [monger.core :as mg]
            [monger.collection :as mc]
            [monger.query :refer :all]
            [monger.operators :refer [$each $addToSet $pull $in $all $or $regex]]
            [monger.conversion :refer [to-db-object from-db-object]]
            [ring.util.response :refer [response bad-request]]
            [demo04.utils :refer [parse-int getenv]])
  (:import (com.mongodb DuplicateKeyException)
           (org.bson.types ObjectId)))



(def MONGO_HOST (getenv "MONGO_HOST" "10.126.21.136"))
(def DB_NAME (getenv "MONGO_DB" "demo04"))

(def ^:private conn (mg/connect {:host MONGO_HOST}))

(defn- convert-to-map [obj]
  (let [m (from-db-object obj true)]
    (->
      (dissoc m :_id)
      (assoc :id (str (:_id m)))
      )))

(defn- rename-user-profile [m]
  (clojure.set/rename-keys m {:name :displayName :addr :address}))

(defn- page-response [p-size total p-idx items]
  (response {
             :pageSize          p-size
             :pageCount         (quot (+ p-size total) p-size)
             :totalCount        total
             :currentPageNumber p-idx
             :customerList      items
             })
  )


(defn user-search [p-idx s-word]
  (let [db (mg/get-db conn DB_NAME)
        coll "user_profile"
        p-size 20
        s (if s-word {$or [
                           {:name {$regex s-word}}
                           {:phone {$regex s-word}}
                           {:email {$regex s-word}}]} {})
        total (mc/count db coll s)
        ]
    (->> (with-collection db coll
                          (find s)
                          (fields [:name :phone :email :sex :birthday])
                          (paginate :page p-idx :per-page p-size))
         (map convert-to-map)
         (map rename-user-profile)
         (page-response p-size total p-idx)
         )))

(defn user-list [request]
  (user-search (parse-int (get-in request [:params :pageNumber]) 0)
               (get-in request [:params :search])))

(defn new-tag [request]
  (let [db (mg/get-db conn DB_NAME)
        coll "tags"]
    (try
      (response (convert-to-map (mc/insert-and-return db coll {:name (get-in request [:body :name])})))
      (catch DuplicateKeyException e
        (bad-request (.getMessage e))
        ))
    ))

(defn all-tag [request]
  (let [db (mg/get-db conn DB_NAME)
        coll "tags"]
    (response (mapv convert-to-map (mc/find db coll)))
    )
  )

(defn update-tag [request]
  (let [db (mg/get-db conn DB_NAME)
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
  (let [db (mg/get-db conn DB_NAME)
        coll "user_profile"]
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
  (let [db (mg/get-db conn DB_NAME)
        coll "groups"]
    (response (mapv convert-to-map (mc/find db coll)))
    ))


(defn new-group [request]
  (let [db (mg/get-db conn DB_NAME)
        coll "groups"]
    (try
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
  (let [db (mg/get-db conn DB_NAME)
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
  (if-let [oid (try-parse-oid id)]
    (-> (mc/find-map-by-id (mg/get-db conn DB_NAME) "groups" oid)
        (:tags))))

(defn- tag-vec [request]
  (let [m (:params request)]
    (cond
      (:t m) (split-string (:t m))
      (:g m) (get-group-tags (:g m)))))

(defn query [request]
  (let [db (mg/get-db conn DB_NAME)
        coll "user_profile"]
    (if-let [ids (tag-vec request)]
      (response {:count (mc/count db coll {:tags {$all (vec ids)}})
                 :items (->>
                          (with-collection db coll
                                           (find {:tags {$all (vec ids)}})
                                           (limit 50))
                          (mapv convert-to-map))})
      (response []))))

(defn user-info [request]
  (let [db (mg/get-db conn DB_NAME)
        coll "user_profile"
        id_str (get-in request [:params :id])
        fds [:name :phone :email :sex :birthday :tags :addr]]
    (if-let [id (try-parse-oid id_str)]
      (response (rename-user-profile (convert-to-map (mc/find-map-by-id db coll id fds))))
      (response {}))))