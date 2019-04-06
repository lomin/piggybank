(ns me.lomin.accounting-piggybank.interpreter.core
  (:require [me.lomin.accounting-piggybank.accounting.core :as db]
            [me.lomin.accounting-piggybank.interpreter.properties :as props]))

(def IN-MEMORY-VALUE [:balance :amount])
(def IN-MEMORY-EVENTS [:balance :events])

(defn get-local-variable [state {k :key} v-name]
  (get-in state [k v-name]))

(defn set-local-variable [state {k :key} v-name v-value]
  (assoc-in state [k v-name] v-value))

(defn state-write [state {k :key value :amount}]
  (-> state
      (update-in IN-MEMORY-VALUE + value)
      (update-in IN-MEMORY-EVENTS conj k)))

(defn check-negative-balance [state {value :amount :as data}]
  (if (neg? (+ value (get-in state IN-MEMORY-VALUE)))
    (set-local-variable state data :check-failed true)
    state))

(defn db-read [state data]
  (set-local-variable state data :last-document (db/get-last-document state)))

(defn db-write [state {k :key value :amount :as data}]
  (let [last-document (get-local-variable state data :last-document)
        self-link (:self last-document)
        updated-document (db/add-counter-value last-document k value)]
    (-> state
        (db/overwrite-document-by-link self-link updated-document)
        (set-local-variable data :last-document updated-document))))

(defn db-add-new-document [state {k :key :as data}]
  (let [last-document (get-local-variable state data :last-document)
        [branch-id] (:self last-document)]
    (db/insert-new-document state [branch-id k])))

(defn db-link-to-new-document [state {k :key :as data}]
  (let [last-document (get-local-variable state data :last-document)
        [branch-id :as self-link] (:self last-document)]
    (db/overwrite-document-by-link state
                                   self-link
                                   (assoc last-document :next [branch-id k]))))

(defn db-gc-new-branch [state data]
  (let [branch-init-link (db/make-branch-init-link data)]
    (db/insert-new-document state
                            branch-init-link
                            [(get-in state IN-MEMORY-EVENTS)
                             (get-in state IN-MEMORY-VALUE)])))

(defn db-gc-link-to-new-branch [state data]
  (let [[branch-id :as branch-init-link] (db/make-branch-init-link data)]
    (-> state
        (assoc-in [:accounting :meta :meta-document :first] branch-init-link)
        (assoc-in [:accounting :meta :meta-document branch-id] branch-init-link))))

(defn check-failed? [state data]
  (get-local-variable state data :check-failed))

(defn interpret-event [state [event-type {id :id :as data}]]
  (let [data-with-key (assoc data :key (db/id->key id))]
    (if (check-failed? state data-with-key)
      state
      (condp = event-type
        :user (check-negative-balance state data-with-key)
        :db-read (db-read state data-with-key)
        :db-write (db-write state data-with-key)
        :state-write (state-write state data-with-key)
        :db-link-to-new-document (db-link-to-new-document state data-with-key)
        :db-add-new-document (db-add-new-document state data-with-key)
        :db-gc-new-branch (db-gc-new-branch state data-with-key)
        :db-gc-link-to-new-branch (db-gc-link-to-new-branch state data-with-key)
        state))))

(def inc-or-0 (fnil inc 0))

(defn inc-check-count [state]
  (update state :check-count inc-or-0))

(defn add-previous-state [state previous-state]
  (assoc state :previous-state (dissoc previous-state :previous-state)))

(defn add-property-violation [state violation timeline]
  (assoc state :property-violated {:name violation :timeline timeline}))

(defn check-properties [state timeline progress-bar]
  (when progress-bar (progress-bar))
  (if-let [violation (props/any-property-violation state)]
    (reduced (add-property-violation state violation timeline))
    (inc-check-count state)))

(defn interpret-timeline
  ([state timeline] (interpret-timeline nil state timeline))
  ([progress-bar state timeline]
   (reduce (fn [state* event]
             (-> state*
                 (interpret-event event)
                 (add-previous-state state*)
                 (check-properties timeline progress-bar)))
           state
           timeline)))