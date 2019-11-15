(ns demo04.user-tag
  (:refer-clojure :exclude [sort find])
  (:require [clojure.string :as string]
            [monger.core :as mg]
            [monger.collection :as mc]
            [monger.credentials :as mcred]
            [monger.query :refer :all]
            [semantic-csv.core :as sc]
            [clojure.java.io :as io]
            [clojure-csv.core :as csv]
            [cheshire.core :as json]
            [clj-time.core :as t])
  (:import (org.joda.time DateTime)))

(def data-age [["5dcbbada19cd444abd6a2c82" 50 nil]
               ["5dcbbada19cd444abd6a2c80" 41 49]
               ["5dcbbada19cd444abd6a2c7f" 24 30]
               ["5dcbbada19cd444abd6a2c7e" 18 23]
               ["5dcbbada19cd444abd6a2c7d" 0 17]])

(defn tag-age [m]
  (let [get-tag (fn [n] (some (fn [[t s e]]
                                (if e
                                  (when (<= s n e) t)
                                  (when (<= s n) t))) data-age))]
    (cond
      (:age m) (get-tag (:age m))
      (and (:birthday m)
           (instance? DateTime (:birthday m))) (get-tag (- (t/year (t/now)) (t/year (:birthday m)))))))

(defn load-cities []
  (let [conn (mg/connect-with-credentials "10.126.21.136" (mcred/create "reader" "demo04" "!!123abc"))
        db (mg/get-db conn "demo04")]
    (doall (mc/find-maps db "cities"))))

(defn load-streets []
  (with-open [in-file (io/reader "data/streets.csv")]
    (->> (csv/parse-csv in-file)
         (sc/remove-comments)
         (sc/mappify)
         doall)))

(defn load-user []
  (let [conn (mg/connect-with-credentials "10.126.21.136" (mcred/create "reader" "demo04" "!!123abc"))
        db (mg/get-db conn "demo04")]
    (mc/find-maps db "user_profile")))

(defn mapping-city
  "get province and city tag by street from data"
  [m data-city]
  (if m
    (let [e (fn [it] (if (or (= (:code it) (:provinceCode m))
                             (= (:code it) (:cityCode m))
                             (= (:code it) (:code m))) it))
          p (some e data-city)
          c (some e (:cities p))]
      (remove nil? [(if p [(str (:_id p)) (:name p)])
                    (if c [(str (:id c)) (:name c)])]))))

(defn string-match? [s b]
  (if (< 1 (count b))
    (or (string/includes? s b)
        (string/includes? s (subs b 0 (- (count b) 1))))))

(defn find-street
  "find first street name in address"
  [s streets]
  (some (fn [m]
          (if (string-match? s (:name m))
            m)) streets))

(defn find-city
  "city or province name in address"
  [s data]
  (some (fn [m]
          (cond
            (string-match? s (:name m)) m
            (:cities m) (find-city s (:cities m))
            (:areas m) (find-city s (:areas m))))
        data))

(defn tag-city [s data-city data-street]
  (if-let [city (find-city s data-city)]
    (mapping-city city data-city)
    (mapping-city (find-street s data-street) data-city)))

(defn analysis [data-city data-street m]
  (let [age (tag-age m)
        addr (tag-city (:addr m) data-city data-street)
        addr (if (nil? addr) [["no_loc"]] addr)
        tags (remove nil? (conj (map first addr) age))]
    (if (not-empty tags) {:_id (:_id m) :tags (set (concat tags (:tags m)))})))

(defn process-tag []
  (let [street (load-streets)
        cities (load-cities)
        u (load-user)]
    (with-open [wr (io/writer "user-tags.json")]
      (doseq [it (pmap (partial analysis cities street) u)]
        (.write wr (format "{\"_id\" : ObjectId(\"%s\"),\"tags\":[\"" (str (:_id it))))
        (.write wr (string/join "\",\"" (:tags it)))
        (.write wr "\"]}\n")))))

(defn cant-analysis [users data-city data-street]
  (map :addr (remove (partial analysis data-city data-street) users)))