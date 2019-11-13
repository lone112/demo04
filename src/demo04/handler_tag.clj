(ns demo04.handler-tag
  (:refer-clojure :exclude [sort find any?])
  (:use [demo04.user-groups :only (check-data group-to-tags)])
  (:require clojure.set
            clojure.string
            monger.joda-time
            monger.json
            cheshire.generate
            [clj-time.core :as t]
            [monger.core :as mg]
            [monger.collection :as mc]
            [monger.query :refer :all]
            [monger.operators :refer [$each $addToSet $pull $in $all $or $regex $gte $lt $lte] :as op]
            [monger.conversion :refer [to-db-object from-db-object]]
            [monger.credentials :as mcred]
            [ring.util.response :refer [response bad-request]]
            [demo04.utils :refer [parse-int getenv drop-time same-date?]]
            [clj-time.core :as t])
  (:import (com.mongodb DuplicateKeyException)
           (org.bson.types ObjectId)))

(def DB_NAME (getenv "MONGO_DB" "demo04"))

(defn init-conn []
  (let [host (getenv "MONGO_HOST" "10.126.21.136")
        user (getenv "MONGO_USER" "reader")
        pwd (getenv "MONGO_PWD" "!!123abc")
        uri (getenv "MONGO_URI")
        uri (if (= \" (first uri)) (subs uri 1 (- (count uri) 2)) uri)]
    (if uri
      (:conn (mg/connect-via-uri uri))
      (mg/connect-with-credentials host (mcred/create user DB_NAME pwd)))))

(defn get-database [name]
  (mg/get-db (init-conn) name))

(defonce ^:private db (delay (get-database DB_NAME)))

(defn- map-object-id-string [m]
  (into {} (for [[k v] m]
             (cond (= :_id k) [:id (str v)]
                   (instance? ObjectId v) [k (str v)]
                   :else [k v]))))

(defn- rename-user-profile [m]
  (clojure.set/rename-keys m {:name :displayName :addr :address}))

(defn- page-response [p-size total p-idx items]
  (response {:pageSize          p-size
             :pageCount         (quot (+ p-size total) p-size)
             :totalCount        total
             :currentPageNumber p-idx
             :customerList      items}))

(defn user-search [p-idx s-word]
  (let [coll "user_profile"
        p-size 10
        s (if s-word {$or [{:name {$regex s-word}}
                           {:phone {$regex s-word}}
                           {:email {$regex s-word}}]} {})
        total (mc/count @db coll s)]
    (->> (with-collection
           @db coll
           (find s)
           (fields [:name :phone :email :sex :city :orderCount :amount :addr])
           (paginate :page p-idx :per-page p-size))
         (map map-object-id-string)
         (map rename-user-profile)
         (page-response p-size total p-idx))))

(defn user-list [request]
  (user-search (parse-int (get-in request [:params :pagenumber]) 0)
               (get-in request [:params :search])))

(defn new-tag [request]
  (let [coll "tags"]
    (try
      (response (map-object-id-string (mc/insert-and-return @db coll {:name (get-in request [:body :name])})))
      (catch DuplicateKeyException e
        (bad-request (.getMessage e))))))

(defn all-tag [request]
  (response (mc/aggregate @db "tags" [{op/$match {:system 0}}
                                      {op/$project {:_id  0
                                                    :id   "$_id"
                                                    :name 1}}])))

(defn update-tag [request]
  (let [coll "tags"
        id (get-in request [:params :id])
        name (get-in request [:body :name])]

    (try
      (mc/update-by-id @db coll (ObjectId. id) {:name name})
      (response "OK")
      (catch DuplicateKeyException e
        (bad-request (.getMessage e))))))

(defn batch-update [request]
  (let [coll "user_profile"]
    (doseq [{:keys [id tags del]} (:body request)]
      (if (seq tags)
        (mc/update-by-id @db coll (ObjectId. id) {$addToSet {:tags {$each tags}}}))
      (if (seq del)
        (mc/update-by-id @db coll (ObjectId. id) {$pull {:tags {$in del}}})))
    (response "OK")))

(defn all-group [request]
  (let [coll "groups"]
    (response (map map-object-id-string (mc/find @db coll)))))

(defn new-group [request]
  (let [coll "groups"
        m (:body request)
        error (check-data m)]
    (try
      (if (seq? error)
        (response {:errMsg error})
        (response (clojure.set/rename-keys (mc/insert-and-return @db coll m) {:_id :id})))
      (catch DuplicateKeyException e
        (bad-request (.getMessage e))))))

(defn- try-parse-oid [s]
  (try
    (ObjectId. s)
    (catch Exception _
      nil)))

(defn del-group [request]
  (let [coll "groups"]
    (if-let [oid (try-parse-oid (get-in request [:params :id]))]
      (response {:status "OK"
                 :msg    (.toString (mc/remove-by-id @db coll oid))})
      (response {:status "OK"}))))

(defn- split-string [s]
  (->> (clojure.string/split s #",")
       (map clojure.string/trim)
       (filter #(< 1 (count %)))))

(defn- get-group-tags [id]
  (if-let [oid (try-parse-oid id)]
    (:tags (mc/find-map-by-id @db "groups" oid))))

(defn- tag-vec [request]
  (let [m (:params request)]
    (cond
      (:t m) (split-string (:t m))
      (:g m) (get-group-tags (:g m)))))

(defn query [request]
  (let [coll "user_profile"]
    (if-let [ids (tag-vec request)]
      (response {:count (mc/count @db coll {:tags {$all (vec ids)}})
                 :items (->> (with-collection @db coll (find {:tags {$all (vec ids)}}) (limit 50))
                             (map map-object-id-string))})
      (response []))))

(defn user-info [request]
  (let [coll "user_profile"
        id_str (get-in request [:params :id])
        fds [:name :phone :email :sex :birthday :tags :addr]]
    (if-let [id (try-parse-oid id_str)]
      (response (assoc (rename-user-profile (map-object-id-string (mc/find-map-by-id @db coll id fds))) :tags ["金牌会员" "高消费" "参与双十一" "活跃用户"]))
      (response {}))))

(defn query-activity [uid start type]
  (let [q {:uid uid :date {$lte start}}
        q1 (if type (assoc q :activityType type) q)]
    (with-collection
      @db
      "activities"
      (find q1)
      (fields [:date :activityType :content])
      (sort {:date -1}))))

(defn user-activity [request]
  (let [id_str (get-in request [:params :id])
        typ (get-in request [:params :filter])
        st (get-in request [:params :startdate] (t/now))]
    (if-let [uid (try-parse-oid id_str)]
      (loop [[el & colls] (query-activity uid (drop-time st) typ) items [] c 10]
        (if (and el (pos? c))
          (if (same-date? (:date el) (:date (last items)))
            (recur colls (conj items el) c)
            (recur colls (conj items el) (dec c)))
          (response {:id id_str :details (map map-object-id-string items)}))))))

(defn user-score [request]
  (let [id_str (get-in request [:params :id])
        coll "user_profile"]
    (if-let [uid (try-parse-oid id_str)]
      (if-let [amer_id (:amer_id (mc/find-map-by-id @db coll uid [:amer_id]))]
        (-> (mc/find-map-by-id @db "user_scores" amer_id)
            (clojure.set/rename-keys {:_id :id})
            (assoc :id id_str)
            (response))))))

(defn user-prefer [request]
  (let [id_str (get-in request [:params :id])
        coll "user_prefer"]
    (if-let [id (try-parse-oid id_str)]
      (-> (mc/find-map-by-id @db coll id)
          (assoc :id id_str)
          (dissoc :_id)
          response))))

(defn user-profile [request]
  ;(prn (:identity request)) claims
  (response {:id          "ac0001"
             :displayName "Admin"}))

(defn cities [request]
  (let [coll "cities"]
    (response {:districts (mc/aggregate @db "cities" [{op/$project {"cities.areas"        0
                                                                    "cities.code"         0
                                                                    "cities.provinceCode" 0
                                                                    :code                 0}}
                                                      {op/$project {:_id      0
                                                                    :id       "$_id"
                                                                    :province "$name"
                                                                    :cities   1}}])
               :brands    (mc/aggregate @db "brands" [{op/$project {:name "$brand"
                                                                    :_id  0
                                                                    :id   "$_id"}}])
               :products  (mc/aggregate @db "products" [{op/$project {:name 1
                                                                      :_id  0
                                                                      :id   "$_id"}}])})))


(defn- tags-for-group []
  (mc/find-maps @db "tags" {:system 1}))

(defn group-user-count [request]
  (let [m (:body request)
        errors (check-data m)]
    (if (seq? errors)
      (response {:status 400
                 :errMsg errors})
      (let [all-tags (tags-for-group)
            data-age (filter #(= "age" (:category %)) all-tags)
            data-avg (filter #(= "avg" (:category %)) all-tags)
            data-total (filter #(= "total" (:category %)) all-tags)
            ]
        (group-to-tags m data-age data-avg data-total)
        )
      )
    ))