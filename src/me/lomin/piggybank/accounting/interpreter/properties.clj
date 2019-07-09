(ns me.lomin.piggybank.accounting.interpreter.properties
  (:require [clojure.set :as set]
            [me.lomin.piggybank.accounting.document-db.core :as accounting]))

(defn- collect-links [f state]
  (reduce-kv f
             #{}
             (dissoc (:accounting state) [:cash-up :meta])))

(defn- all-existing-documents [state]
  (collect-links (fn [acc [_ cash-up-id] documents]
                   (into acc (map (fn [[_ document-id]]
                                    {:cash-up-id  cash-up-id
                                     :document-id document-id})
                                  (keys documents))))
                 state))

(defn- all-linked-documents [state]
  (conj (collect-links (fn [acc _ documents]
                         (into acc (map (fn [document]
                                          (:next document))
                                        (vals documents))))
                       state)
        (accounting/get-meta-start-link state)))

(defn all-links-exist? [state]
  (set/superset? (all-existing-documents state)
                 (all-linked-documents state)))

(def get-summands (comp (mapcat :transfers) (map second)))
(def get-event-ids (comp (mapcat :transfers) (mapcat first)))

(defn add-all-summands [all-documents]
  (transduce get-summands + all-documents))

(defn db-state>=0? [state]
  (<= 0 (add-all-summands (accounting/follow-next-links state))))

(defn all-event-ids-from-cash-ups [universe]
  (into #{} get-event-ids (accounting/follow-next-links universe)))

(defn all-event-ids-from-balance [universe]
  (get-in universe [:balance :processes]))

(defn lost-updates? [universe]
  (not (set/superset? (all-event-ids-from-cash-ups universe)
                      (all-event-ids-from-balance universe))))

(defn branches-come-to-different-results? [state]
  (apply not=
         (map (fn [[_ link]]
                (add-all-summands
                 (accounting/follow-next-links state
                                               (accounting/get-document-by-link state link))))
              (accounting/get-meta-document state))))

(defn- collect-documents [cash-up-id branch]
  (reduce-kv (fn [s k v]
               (conj s (-> v
                           (assoc ::cash-up cash-up-id)
                           (assoc ::document k))))
             #{}
             branch))

(defn- collect-branches [db]
  (reduce-kv (fn [s k v]
               (into s (collect-documents k v)))
             #{}
             db))

(defn more-than-one-document-change-per-time-slot? [state]
  (boolean (and (seq (:history state))
                (< 1 (count (set/difference (collect-branches (:accounting state))
                                            (collect-branches (-> state
                                                                  (:history)
                                                                  (first)
                                                                  (:accounting)))))))))

(defn any-property-violation [{:keys [universe]}]
  (cond
    (not (all-links-exist? universe)) :all-links-must-point-to-an-existing-document
    (not (db-state>=0? universe)) :db-state-must-always-be>=0
    (lost-updates? universe) :there-must-be-no-lost-updates
    (branches-come-to-different-results? universe) :all-branches-must-come-to-the-same-result
    (more-than-one-document-change-per-time-slot? universe) :there-must-be-only-one-document-change-per-timeslot))
