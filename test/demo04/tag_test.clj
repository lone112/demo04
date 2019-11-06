(ns demo04.tag-test
  (:use midje.sweet)
  (:require [clojure.string :as str]))

(facts "about `split`"
       (str/split "a/b/c" #"/") => ["a" "b" "c"]
       (str/split "" #"irrelvant") => [""]
       (str/split "no regexp matches" #"a+\s+[ab]") => ["no regexp matches"])
