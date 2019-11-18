(ns demo04.handler-tag
  (:refer-clojure :exclude [sort find any?])
  (:use [demo04.user-groups :only (check-group group-tag-tiny)])
  (:require clojure.set
            clojure.string
            monger.joda-time
            monger.json
            [cheshire.core :as json]
            [clj-time.core :as t]
            [monger.core :as mg]
            [monger.collection :as mc]
            [monger.query :refer :all]
            [monger.operators :refer [$each $addToSet $pull $in $all $or $regex $gte $lt $lte $in] :as op]
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

(defn- try-parse-oid [s]
  (try
    (ObjectId. s)
    (catch Exception _
      nil)))

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
    (response (map (fn [m]
                     (assoc (clojure.set/rename-keys m {:_id :id}) :count (rand-int 1000))) (mc/find-maps @db coll)))))

(defn get-group-by-id [id]
  (if-let [oid (try-parse-oid id)]
    (response (clojure.set/rename-keys (mc/find-map-by-id @db "groups" oid) {:_id :id}))))

(defn new-group [request]
  (let [m (:body request)]
    (try
      (if-let [error (check-group m)]
        (response {:errMsg error})
        (response (clojure.set/rename-keys (mc/insert-and-return @db "groups" m) {:_id :id})))
      (catch DuplicateKeyException e
        (bad-request (.getMessage e))))))

(defn update-group [request id]
  (if-let [oid (try-parse-oid id)]
    (if-let [error (check-group (:body request))]
      (response {:errMsg error})
      (do
        (mc/update-by-id @db "groups" oid (:body request))
        (response {:status 200})))))

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
                                                                    :id   "$_id"}}])}))


(defn- tags-for-group []
  (mc/find-maps @db "tags" {:system 1}))

(defn convert-to-match [it]
  (cond
    (= 1 (count it)) {op/$match {:tags (first it)}}
    (coll? it) {op/$match {:tags {"$all" it}}}
    (some? it) {op/$match {:tags it}}
    ))

(defn filter-tags [attr tags]
  (map (fn [{:keys [_id name]}]
         [(str _id) name]) (filter #(= attr (:category %)) tags)))

(defn build-group-tags [m preset-tags]
  (let [data-age (filter-tags "age" preset-tags)
        data-avg (filter-tags "avg" preset-tags)
        data-total (filter-tags "total" preset-tags)]
    (group-tag-tiny m data-age data-avg data-total)))

(defn lookup [coll p-tags b-tags]
  (if (or p-tags b-tags)
    (let [m {"$match" {"lk_tags" {"$not" {"$size" 0}}}}
          m (if p-tags (assoc-in m ["$match" "lk_tags.products" "$all"] p-tags) m)
          m (if b-tags (assoc-in m ["$match" "lk_tags.brands" "$all"] b-tags) m)]

      [{"$lookup" {:from         coll
                   :localField   "amer_id"
                   :foreignField "_id"
                   :as           "lk_tags"}}
       m])))

(defn query-group-count [m]
  (let [all-tags (tags-for-group)
        gt (build-group-tags m all-tags)
        matchs [(convert-to-match (:sex gt))
                (convert-to-match (:ages gt))
                (convert-to-match (:tags gt))
                (convert-to-match (:spend-avg gt))
                (convert-to-match (:spend-total gt))
                (lookup "user_tags_1y" (:behavior-products gt) (:behavior-brands gt))
                {"$count" "count"}]
        query (flatten (remove nil? matchs))]
    (prn "group query:" query)
    (mc/aggregate @db "user_profile" query)))

(defn handel-group-count [request]
  (let [m (:body request)
        ]
    (if-let [errors (check-group m)]
      (response {:status 400
                 :errMsg errors})
      (response (query-group-count m))
      )))
