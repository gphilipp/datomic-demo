(require '[datomic.api :as d])
(use 'clojure.pprint)
(require '[datomic-demo.util :as util])


;; this is the database uri
(def uri "datomic:mem://deals")

;; create the database
(d/delete-database uri)
(d/create-database uri)

;; connecting to database
(def conn (d/connect uri))

;; install database schema
(d/transact conn
            ;; deal
            [{:db/id #db/id[:db.part/db]
              :db/ident :deal/name
              :db/unique :db.unique/identity
              :db/valueType :db.type/string
              :db/cardinality :db.cardinality/one
              :db.install/_attribute :db.part/db}
             {:db/id #db/id[:db.part/db]
              :db/ident :deal/counterpart
              :db/valueType :db.type/string
              :db/cardinality :db.cardinality/one
              :db.install/_attribute :db.part/db}
             {:db/id #db/id[:db.part/db]
              :db/ident :deal/amount
              :db/valueType :db.type/long
              :db/cardinality :db.cardinality/one
              :db.install/_attribute :db.part/db}
             {:db/id #db/id[:db.part/db]
              :db/ident :deal/received-at
              :db/valueType :db.type/instant
              :db/cardinality :db.cardinality/one
              :db.install/_attribute :db.part/db}
             ])

;; add a deal with Argentina for a bridge
(def tx-result (d/transact conn
                           [{:db/id #db/id[:db.part/user]
                             :deal/name "Rosario Bridge"
                             :deal/counterpart "Argentina Government"
                             :deal/amount 150000000
                             :deal/received-at #inst "2015-01-15T10:13Z"}]))


;; db is a value, some sort of "git tag"
(def db-at-t0 (:db-after @tx-result))
(def t0 (util/last-tx-time db-at-t0))


;; find all datoms related to the deal
(d/q '[:find ?e ?a ?v ?t ?added
       :where
       [?e ?a ?v ?t ?added]
       [?e :deal/name "Rosario Bridge"]]
     db-at-t0)


;; find the whole deal at once
(def bridge-deal (ffirst (d/q '[:find (pull ?e [*])
                                :where
                                [?e :deal/name]]
                              db-at-t0)))

;; get the deal's entity id
(def bridge-deal-entityid (:db/id bridge-deal))

;; change the bridge deal's amount to 170 M
(def tx-result (d/transact conn
                           [{:db/id bridge-deal-entityid
                             :deal/amount 170000000
                             :deal/received-at #inst "2015-02-03T12:45Z"}]))

;; new db snapshot
(def db-at-t1 (:db-after @tx-result))
(def t1 (util/last-tx-time db-at-t1))



;; find all datoms related to the deal
(d/q '[:find ?e ?a ?v ?t ?added
       :where
       [?e ?a ?v ?t ?added]
       [?e :deal/name "Rosario Bridge"]]
     db-at-t1)


;; find all history of this deal (detailed)
(def price-history
  (pprint
    (seq (d/q '[:find ?amount ?name ?deal-date ?added ?ts
                :in $ ?e
                :where
                [?e :deal/amount ?amount ?t ?added]
                [?e :deal/name ?name]
                [?e :deal/received-at ?deal-date]
                [?t :db/txInstant ?ts]]
              (d/history db-at-t1)
              bridge-deal-entityid))))


;; find all history of this deal (detailed)
(def detailed-bridge-history (d/q '[:find ?e ?a ?v ?tx ?added
                                    :in $ ?e
                                    :where
                                    [?e ?a ?v ?tx ?added]]
                                  (d/history db-at-t1)
                                  bridge-deal-entityid))

;; group datoms by transaction
(binding [*print-right-margin* 100]
  (-> (group-by #(get % 3) detailed-bridge-history)
      pprint))


(d/q '[:find ?diff
       :in $1 $2
       :where
       [$1 ?e :deal/amount ?a1]
       [$2 ?e :deal/amount ?a2]
       [(- ?a1 ?a2) ?diff]]
     db-at-t1 db-at-t0)


(def t-between-t0-and-t1 (util/add-millis t0 10))
(def db-between-t0-and-t1 (d/as-of (d/db conn) t-between-t0-and-t1))



(ffirst (d/q '[:find (pull ?e [*])
               :where
               [?e :deal/name]]
             db-between-t0-and-t1))


(def tx-sim-result
  (d/with db-between-t0-and-t1
          [{:db/id bridge-deal-entityid
            :deal/name "Rosario Bridge"
            :deal/counterpart "Argentina Government"
            :deal/amount 180000000
            :deal/received-at #inst "2015-01-15T10:13Z"}]
          ))


(def db-sim (:db-after tx-sim-result))


(ffirst (d/q '[:find (pull ?e [*])
               :where
               [?e :deal/name]]
             db-sim))

(def tx-another-sim
  (d/with db-sim
          [{:db/id bridge-deal-entityid
            :deal/amount 190000000
            :deal/received-at #inst "2015-02-27T10:13Z"}]))

(def db-another-sim (:db-after tx-another-sim))

(util/list-datoms (:db-after tx-another-sim))

(ffirst (d/q '[:find (pull ?e [*])
               :where
               [?e :deal/name]]
             db-another-sim))








