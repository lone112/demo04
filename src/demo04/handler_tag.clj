(ns demo04.handler-tag
  (:require [monger.core :as mg]
            [monger.collection :as mc]
            [ring.util.response :refer [response]])
  (:import [com.mongodb MongoOptions ServerAddress]))

(defn handler [request]
  (let [conn (mg/connect)
        db (mg/get-db conn "demo04")
        coll "user_profile"
        cr (mc/find db coll)
        items (mapv (fn [m]
                      {:id (str (get m "_id"))
                       :name (get m "name")
                       :tags (get m "tags")}) cr)
        ]
    (response items))
  )
