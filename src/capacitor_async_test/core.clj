(ns capacitor-async-test.core
  (:require [capacitor.core :as influx]
            [capacitor.async :as influx-async]
            [clojure.core.async :as async]))

(def client
  (influx/make-client {:db "my-new-db"}))

;; Define an InfluxDB client (see basic.clj for example)
(def c
  (influx/make-client {
    :db       "my-new-db"
    :username "myuser"
    :password "mypassword" }))

;; Make a channel to buffer incoming events
(def events-in (influx-async/make-chan))

;; Make a channel to collect post responses
(def resp-out (influx-async/make-chan))


(def query-00
  (str
    "SELECT COUNT(email) "
    "FROM logins "
    "GROUP BY time(1s) "
    "WHERE email =~ /.*gmail\\.com/"))

(defn get-query
  []
  (influx/get-query c query-00))

(defn -main
  []
  (async/thread ;; silly
    (influx/create-db client)
    (influx/create-db-user client "myuser" "mypassword")
    ;; Start the run loop with a batch size of max 10 events and max 1 seconds
    (influx-async/run! events-in resp-out c 10 1000)

    ;; Enqueue events
    (influx-async/enqueue events-in {
      :series "logins"
      :email  "paul@gmail.com" })

    (influx-async/enqueue events-in {
      :series "signups"
      :email  "john@gmail.com" })

    (influx-async/enqueue events-in {
      :series "logins"
      :email  "ringo@gmail.com" }))

  (Thread/sleep 2000)
  (prn (get-query)))
