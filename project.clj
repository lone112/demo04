(defproject demo04 "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/tools.logging "0.5.0"]
                 [org.clojure/data.csv "0.1.4"]
                 [ring/ring-defaults "0.3.2"]
                 [ring/ring-core "1.7.1"]
                 [ring/ring-jetty-adapter "1.7.1"]
                 [ring/ring-devel "1.7.1"]
                 [ring/ring-json "0.5.0"]
                 [ring-cors "0.1.13"]
                 [compojure "1.6.1"]
                 [com.novemberain/monger "3.5.0"]
                 [cheshire "5.9.0"]
                 [buddy/buddy-auth "2.2.0"]
                 [clj-time "0.13.0"]
                 ;; https://mvnrepository.com/artifact/org.mongodb/mongodb-driver
                 [org.mongodb/mongodb-driver "3.11.1"]
                 ]

  :plugins [[lein-ring "0.12.5"]]
  :ring {:handler demo04.handler/app}
  :main demo04.handler
  :profiles {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                                  [ring/ring-mock "0.4.0"]
                                  [midje "1.9.9" :exclusions [org.clojure/clojure]]]}})
