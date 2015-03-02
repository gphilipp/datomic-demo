(ns datomic-demo.util
  [:require [datomic.api :as d]]
  (:import (java.util Calendar GregorianCalendar)))

(defn last-tx-time [db]
  (ffirst (d/q '[:find ?time
                 :in $ ?e
                 :where
                 [?e :db/txInstant ?time]]
               db
               (d/entid-at db :db.part/tx (d/basis-t db)))))

(defn list-datoms [db]
  (->> (seq (d/datoms db :eavt))
       (filter #(> (:a %) 62))))

(defn add-millis
  "Takes a date, add milliseconds to it and returns a new date"
  [d millis]
  (let [gc (Calendar/getInstance)]
    (doto gc
      (.setTime d)
      (.add Calendar/MILLISECOND millis)
      )
    (.getTime gc)))

