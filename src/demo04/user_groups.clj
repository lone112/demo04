(ns demo04.user-groups
  (:require [validateur.validation :refer :all]
            [cheshire.core :as json]))


(defn every-items-has-id [items]
  (every? :id items))

(defn check-number-array [items]
  (and (> 2 (count (filter nil? items)))                    ;; one nil or null
       (even? (count items))                                ;; length must even
       (every? (complement neg-int?) (filter number? items)))) ;; all item type is number

(def basic (validation-set
             (validate-nested
               :basicInfo
               (validation-set
                 (validate-by :age check-number-array :message "age array length must even")
                 (validate-by :districts every-items-has-id)
                 (inclusion-of :sex :allow-nil true :in #{"M" "F" nil} :message "must be one of NULL,")
                 ))))

(def behavior (validation-set
                (validate-nested
                  :behavior
                  (validation-set
                    (validate-by :brands every-items-has-id)
                    (validate-by :products every-items-has-id)
                    (inclusion-of :range :allow-nil true :in #{"7D" "1M" "3M" "6M" "1Y"})
                    ))))
(def prefer (validation-set
              (validate-nested
                :prefer
                (validation-set
                  (validate-by :brands every-items-has-id)
                  (validate-by :products every-items-has-id)))))

(def p (validation-set
         (presence-of :name)
         (validate-by :tags (partial not-any? nil?))
         (inclusion-of :updated :allow-nil true :in #{0 1 7 30})
         ))

(def spend (validation-set
             (validate-nested
               :spend
               (validation-set
                 (validate-by :avg check-number-array :message "avg array length must even")
                 (validate-by :total check-number-array :message "avg array length must even")
                 ))))

(defn check-data [m]
  (let [v (compose-sets p basic behavior prefer spend)]
    (v m)))
