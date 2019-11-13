(ns demo04.user-tag)

(def age-data [
               ["3434l3546l231d" 41]
               ["3434l3546l231c" 31 40]
               ["3434l3546l231b" 19 30]
               ["3434l3546l231a" 0 18]
               ])

(defn age-tag [m]
  (if-let [age (:age m)]
    (some (fn [[t s e]]
            (if e
              (when (<= s age e) t)
              (when (<= s age) t))) age-data)))


(defn analysis [m])