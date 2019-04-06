(ns me.lomin.accounting-piggybank.interpreter.properties
  (:require [clojure.set :as set]
            [me.lomin.accounting-piggybank.accounting.core :as db]))

(defn- collect-links [f state]
  (reduce-kv f
             #{}
             (dissoc (:accounting state) :meta)))

(defn- all-existing-documents [state]
  (collect-links (fn [acc branch-id documents]
                   (into acc (map (fn [document-id]
                                    [branch-id document-id])
                                  (keys documents))))
                 state))

(defn- all-linked-documents [state]
  (conj (collect-links (fn [acc _ documents]
                         (into acc (map (fn [document]
                                          (:next document))
                                        (vals documents))))
                       state)
        (get-in state [:accounting :meta :meta-document :first])))

(defn all-links-exist? [state]
  (set/superset? (all-existing-documents state)
                 (all-linked-documents state)))

(def get-summands (comp (mapcat :transfers) (map second)))
(def get-event-ids (comp (mapcat :transfers) (mapcat first)))

(defn add-all-summands [all-documents]
  (transduce get-summands + all-documents))

(defn db-state>=0? [state]
  (<= 0 (add-all-summands (db/follow-next-links state))))

(def nicht not)

(defn alle-event-ids-über-alle-bücher [universe]
  (into #{} get-event-ids (db/follow-next-links universe)))

(defn alle-event-ids-aus-saldo [universe]
  (get-in universe [:balance :events]))

(defn lost-updates? [universe]
  (nicht (set/superset? (alle-event-ids-über-alle-bücher universe)
                        (alle-event-ids-aus-saldo universe))))

(defn branches-come-to-different-results? [state]
  (apply not=
         (map (fn [[_ link]]
                (add-all-summands
                 (db/follow-next-links state
                                       (db/get-document-by-link state link))))
              (get-in state [:accounting :meta :meta-document]))))

(defn- collect-documents [branch-id branch]
  (reduce-kv (fn [s k v]
               (conj s (-> v
                           (assoc ::branch branch-id)
                           (assoc ::document k))))
             #{}
             branch))

(defn- collect-branches [db]
  (reduce-kv (fn [s k v]
               (into s (collect-documents k v)))
             #{}
             db))

(defn more-than-one-document-change-per-timeslot? [state]
  (boolean (and (:previous-state state)
                (< 1 (count (set/difference (collect-branches (:accounting state))
                                            (collect-branches (get-in state [:previous-state :accounting]))))))))

(defn any-property-violation [state]
  (cond
    (not (all-links-exist? state)) :all-links-must-point-to-an-existing-document
    (not (db-state>=0? state)) :db-state-must-always-be>=0
    (lost-updates? state) :there-must-be-no-lost-updates
    (branches-come-to-different-results? state) :all-branches-must-come-to-the-same-result
    (more-than-one-document-change-per-timeslot? state) :there-must-be-only-one-document-change-per-timeslot))
