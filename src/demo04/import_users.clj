(ns demo04.import-users
  (:require [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [cheshire.core :refer :all]))

(defn convert-to-map [[header & lines]]
  (let [kys (map keyword header)]
    (map (partial zipmap kys) lines)))

(defn omit-empty-or-nil [m]
  (into {} (for [[k v] m :when (and (< 0 (count v)) (not (nil? v)))]
             [k v])))

(defn normalize-phone [{:keys [phone phone2] :as m}]
  (if-let [phones (seq (remove nil? [phone phone2]))]
    (dissoc (assoc m :phone (first phones) :phones (next phones))
            :phone2)
    m))

(defn normalize-email [{:keys [email email2] :as m}]
  (let [emails (remove nil? [email email2])]
    (dissoc (assoc m :email (first emails) :emails (next emails))
            :email2)
    ))


(defn normalize-device [{:keys [device1 device2] :as m}]
  (let [devices (remove nil? [device1 device2])]
    (dissoc (assoc m :device (first devices) :devices (next devices))
            :device2 :device1)
    ))



(defn normalize-by-keys [m kys [k1 ks] & rmkys]
  (let [vls (vals (select-keys m kys))
        its (seq (remove nil? vls))
        omit (fn [it] (apply dissoc it rmkys))]
    (if its
      (-> (assoc m k1 (first its) ks (next its))
          omit
          )
      m)))

(defn normalize-user [m]
  (-> m
      (normalize-by-keys [:email :email2] [:email :emails] :email2)
      (normalize-by-keys [:phone :phone2] [:phone :phones] :phone2)
      (normalize-by-keys [:addr :addr2] [:addr :addrs] :addr2)
      (normalize-by-keys [:device1 :device2] [:device :devices] :device1 :device2)))

(defn export-user [users]
  (with-open [writer (io/writer "D:/demo04/oln_users.json")]
    (doseq [it users] (generate-stream it writer))
    ))

(defn import-user []
  (with-open [reader (io/reader "D:/demo04/oln_users.csv")]
    (->> (csv/read-csv reader)
         convert-to-map
         (filter #(< 3 (count (:id %))))
         (map omit-empty-or-nil)
         (map normalize-user)
         (map omit-empty-or-nil)
         export-user
         )
    ))