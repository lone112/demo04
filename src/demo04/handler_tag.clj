(ns demo04.handler-tag
  (:require [monger.core :as mg]
            [monger.collection :as mc]
            [monger.conversion :refer [to-db-object from-db-object]]
            [ring.util.response :refer [response]]))

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
  (prn (:body request))
  (response {:status "OK"})
  )



(defn all-tag [request]
  (let [conn (mg/connect {:host "10.126.21.136"})
        db (mg/get-db conn "demo04")
        coll "tags"
        cr (mc/find db coll)]
    (response (mapv convert-to-map cr))
    ))

(defn update-tag [request]
  (response {:status "OK"})
  )