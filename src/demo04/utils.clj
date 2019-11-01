(ns demo04.utils)

(defn parse-int [number-string & vls]
  (try (Integer/parseInt number-string)
       (catch Exception _ (first vls))))

(defn getenv [name & vals]
  "Get system env or default val"
  (if-let [v (System/getenv name)]
    v
    (first vals)))

(defn encrypt-string [s]
  "string -> aes -> base64")
(defn decrypt-string [b64-s])