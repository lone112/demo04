(ns demo04.cities
  (:require [clojure.java.io :as io]
            [clojure-csv.core :as csv]
            [semantic-csv.core :as sc]
            [monger.core :as mg]
            [monger.collection :as mc]
            [monger.result :refer [acknowledged?]]
            [monger.credentials :as mcred]
            [ring.util.response :refer [response bad-request]])
  (:import (org.bson.types ObjectId)))

(defn condition-merge [[key1 key2] prop c1 c2]
  (map (fn [m] (assoc m prop (filter #(= (key1 m) (key2 %)) c2))) c1))

(defn read-csv-map [file-name id-key]
  (with-open [in-file (io/reader file-name)]
    (->> (csv/parse-csv in-file)
         (sc/remove-comments)
         (sc/mappify)
         (map (fn [m] (assoc m id-key (ObjectId.))))
         doall)))

(defn city-data []
  (let [cities (read-csv-map "data/cities.csv" :id)
        provinces (read-csv-map "data/provinces.csv" :_id)
        areas (read-csv-map "data/areas.csv" :id)
        conn (mg/connect-with-credentials "10.126.21.136" (mcred/create "reader" "demo04" "!!123abc"))
        db (mg/get-db conn "demo04")]
    (->> (condition-merge [:code :cityCode] :areas cities areas)
         (condition-merge [:code :provinceCode] :cities provinces)
         (mc/insert-batch db "cities"))))
