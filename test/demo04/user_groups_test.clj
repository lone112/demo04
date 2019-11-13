(ns demo04.user-groups-test
  (:use [demo04.user-groups :rename {check-data validate-group-data}])
  (:require [clojure.test :refer :all]
            [midje.sweet :refer :all]
            [cheshire.core :as json]))

(defn load-data []
  (json/parse-stream (clojure.java.io/reader "data/user-group-sample.json") true))

(def json-data (load-data))

(facts "Group data validation"
       (fact "User group name is exists"
             (let [data {:name "hello"}]
               (validate-group-data data) => (just {})))

       (fact "User group name is nil"
             (let [data (dissoc json-data :name)]
               (validate-group-data data) => (contains {:name set?})))

       (fact "Tags is null or empty"
             (validate-group-data (dissoc json-data :tags)) => (just {})
             (validate-group-data (assoc json-data :tags nil)) => (just {})
             (validate-group-data (assoc json-data :tags [])) => (just {}))

       (fact "Tags item invalid"
             (validate-group-data (assoc json-data :tags [nil])) => (contains {:tags set?}))

       (fact "Update option value 0 1 7 30"
             (validate-group-data (assoc json-data :updated 0)) => (just {})
             (validate-group-data (assoc json-data :updated 1)) => (just {})
             (validate-group-data (assoc json-data :updated 7)) => (just {})
             (validate-group-data (assoc json-data :updated 30)) => (just {}))
      (fact "Update value invalid"
            (validate-group-data (assoc json-data :updated 10)) => (contains {:updated set?}))

       (fact "Update is null or empty or not exists"
             (validate-group-data (dissoc json-data :updated)) => (just {})
             (validate-group-data (assoc json-data :updated nil)) => (just {}))
       )


(facts "Basic info validation"
       (fact "Basic info not exists"
             (let [data (dissoc json-data :basicInfo)]
               (validate-group-data data) => (just {})))

       (fact "Sex is null"
             (let [data (assoc-in json-data [:basicInfo :sex] nil)]
               (validate-group-data data) => (just {})))
       (fact "Sex is F"
             (let [data (assoc-in json-data [:basicInfo :sex] "F")]
               (validate-group-data data) => (just {})))
       (fact "Sex is M"
             (let [data (assoc-in json-data [:basicInfo :sex] "M")]
               (validate-group-data data) => (just {})))
       (fact "Sex is other"
             (let [data (assoc-in json-data [:basicInfo :sex] "B")]
               (validate-group-data data) => (contains {[:basicInfo :sex] set?})))
       (fact "Sex not exists"
             (let [data (update-in json-data [:basicInfo] dissoc :sex)]
               (validate-group-data data) => (just {})))

       (fact "Districts not exists"
             (let [data (update-in json-data [:basicInfo] dissoc :districts)]
               (validate-group-data data) => (just {})))
       (fact "districts is empty"
             (let [data (assoc-in json-data [:basicInfo :districts] [])]
               (validate-group-data data) => (just {})))
       (fact "districts is null"
             (let [data (assoc-in json-data [:basicInfo :districts] nil)]
               (validate-group-data data) => (just {})))

       (fact "age is null"
             (let [data (assoc-in json-data [:basicInfo :age] nil)]
               (validate-group-data data) => (just {})))
       (fact "age array invalid length"
             (let [data (assoc-in json-data [:basicInfo :age] [21])]
               (validate-group-data data) => (contains {[:basicInfo :age] set?})))
       )
(facts "Behavior validation"
       (fact "Behavior not exists"
             (let [data (dissoc json-data :behavior)]
               (validate-group-data data) => (just {})))

       (fact "Behavior type is null or not exists"
             (let [data (update-in json-data [:behavior] dissoc :type)]
               (validate-group-data data) => (just {}))
             (let [data (assoc-in json-data [:behavior :type] nil)]
               (validate-group-data data) => (just {})))

       (fact "Behavior brands is null or not exists"
             (let [data (update-in json-data [:behavior] dissoc :brands)]
               (validate-group-data data) => (just {}))
             (let [data (assoc-in json-data [:behavior :brands] nil)]
               (validate-group-data data) => (just {})))

       (fact "Behavior products is null or not exists"
             (let [data (update-in json-data [:behavior] dissoc :products)]
               (validate-group-data data) => (just {}))
             (let [data (assoc-in json-data [:behavior :products] nil)]
               (validate-group-data data) => (just {})))

       (fact "Behavior range is null or not exists"
             (let [data (update-in json-data [:behavior] dissoc :range)]
               (validate-group-data data) => (just {}))
             (let [data (assoc-in json-data [:behavior :range] nil)]
               (validate-group-data data) => (just {})))

       (fact "Behavior range option values 7D/1M/3M/6M/1Y"
             (let [data json-data]
               (validate-group-data (assoc-in data [:behavior :range] "7D")) => (just {})
               (validate-group-data (assoc-in data [:behavior :range] "1M")) => (just {})
               (validate-group-data (assoc-in data [:behavior :range] "3M")) => (just {})
               (validate-group-data (assoc-in data [:behavior :range] "6M")) => (just {})
               (validate-group-data (assoc-in data [:behavior :range] "1Y")) => (just {})))

       (fact "Behavior range invalid value"
             (let [data json-data]
               (validate-group-data (assoc-in data [:behavior :range] "71D")) => (contains {[:behavior :range] set?})))
       )


(facts "Prefer validation"
       (fact "Prefer not exists"
             (let [data (dissoc json-data :prefer)]
               (validate-group-data data) => (just {})))


       (fact "Prefer brand is null or not exists or empty"
             (validate-group-data (update-in json-data [:prefer] dissoc :brands)) => (just {})
             (validate-group-data (assoc-in json-data [:prefer :brands] nil)) => (just {})
             (validate-group-data (assoc-in json-data [:prefer :brands] [])) => (just {}))

       (fact "Prefer products is null or not exists or empty"
             (validate-group-data (update-in json-data [:prefer] dissoc :products)) => (just {})
             (validate-group-data (assoc-in json-data [:prefer :products] nil)) => (just {})
             (validate-group-data (assoc-in json-data [:prefer :products] [])) => (just {}))

       (fact "Prefer products invalid data (without id)"
             (validate-group-data (assoc-in json-data [:prefer :products] [{:name "hello"}])) => (contains {[:prefer :products] set?}))
       )

(facts "Spend ability validation"
       (fact "avg not exists or null or empty"
             (validate-group-data (update-in json-data [:spend] dissoc :avg)) => (just {})
             (validate-group-data (assoc-in json-data [:spend :avg] nil)) => (just {})
             (validate-group-data (assoc-in json-data [:spend :avg] [])) => (just {}))
       (fact "Spend avg invalid data (without id)"
             (validate-group-data (assoc-in json-data [:spend :avg] [0])) => (contains {[:spend :avg] set?}))

       )