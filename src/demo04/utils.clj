(ns demo04.utils
  (:require [clj-time.core :as t]
            [clj-time.coerce :as c]
            [clj-time.format :as f]))

(defn parse-int [number-string & vls]
  (try (Integer/parseInt number-string)
       (catch Exception _ (first vls))))

(defn getenv "Get system env or default val" [name & vals]
  (if-let [v (System/getenv name)]
    v
    (first vals)))

(def built-in-formatter (f/formatters :date))

(defmulti drop-time class)

(defmethod drop-time String [obj]
  (f/parse built-in-formatter obj))

(defmethod drop-time java.util.Date [obj]
  (drop-time (c/from-date obj)))

(defmethod drop-time org.joda.time.DateTime [obj]
  (apply t/date-time ((juxt t/year t/month t/day) obj)))

(defn encrypt-string [s]
  "string -> aes -> base64")
(defn decrypt-string [b64-s])