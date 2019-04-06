(ns me.lomin.accounting-piggybank.interpreter.core
  (:require [me.lomin.accounting-piggybank.accounting.core :as db]
            [me.lomin.accounting-piggybank.accounting.core :as accounting]
            [me.lomin.accounting-piggybank.interpreter.properties :as props]))

(def IN-MEMORY-BALANCE [:balance :amount])
(def IN-MEMORY-PIDS [:balance :processes])

(defn get-local-variable [state {pid :process-id} v-name]
  (get-in state [pid v-name]))

(defn set-local-variable [state {pid :process-id} v-name v-value]
  (assoc-in state [pid v-name] v-value))

(defn state-write [state {pid :process-id value :amount}]
  (-> state
      (update-in IN-MEMORY-BALANCE + value)
      (update-in IN-MEMORY-PIDS conj pid)))

(defn check-negative-balance [state {value :amount :as data}]
  (if (neg? (+ value (get-in state IN-MEMORY-BALANCE)))
    (set-local-variable state data :check-failed true)
    state))

(defn db-read [state data]
  (set-local-variable state data :last-document (db/get-last-document state)))

(defn db-write [state {pid :process-id value :amount :as data}]
  (let [last-document (get-local-variable state data :last-document)
        self-link (:self last-document)
        updated-document (db/add-counter-value last-document pid value)]
    (-> state
        (db/overwrite-document-by-link self-link updated-document)
        (set-local-variable data :last-document updated-document))))

(defn db-add-new-document [state {pid :process-id :as data}]
  (let [last-document (get-local-variable state data :last-document)
        {cash-up-id :cash-up-id} (:self last-document)]
    (db/insert-new-document state (db/make-link cash-up-id (inc pid)))))

(defn db-link-to-new-document [state {pid :process-id :as data}]
  (let [last-document (get-local-variable state data :last-document)
        {cash-up-id :cash-up-id :as self-link} (:self last-document)]
    (db/overwrite-document-by-link state
                                   self-link
                                   (assoc last-document
                                          :next
                                          (accounting/make-link cash-up-id (inc pid))))))

(defn db-gc-new-branch [state data]
  (let [branch-init-link (db/make-branch-init-link data)]
    (db/insert-new-document state
                            branch-init-link
                            [(get-in state IN-MEMORY-PIDS)
                             (get-in state IN-MEMORY-BALANCE)])))

(defn db-gc-link-to-new-branch [state data]
  (db/insert-branch-in-meta state (db/make-branch-init-link data)))

(defn restart [state {:keys [past]}]
  (let [past-state (if (and (:history state) (< 0 past))
                     (nth (:history state) (dec past))
                     state)
        documents (accounting/follow-next-links past-state)
        ids (into #{} props/get-event-ids documents)
        balance (props/add-all-summands documents)]
    (-> state
        (assoc-in IN-MEMORY-BALANCE balance)
        (assoc-in IN-MEMORY-PIDS ids))))

(defn check-failed? [state data]
  (get-local-variable state data :check-failed))

(defn interpret-event [state [event-type data]]
  (if (check-failed? state data)
    state
    (condp = event-type
      :process (check-negative-balance state data)
      :accounting-read (db-read state data)
      :accounting-write (db-write state data)
      :balance-write (state-write state data)
      :accounting-link-to-new-document (db-link-to-new-document state data)
      :accounting-add-new-document (db-add-new-document state data)
      :accounting-gc-new-branch (db-gc-new-branch state data)
      :accounting-gc-link-to-new-branch (db-gc-link-to-new-branch state data)
      :restart (restart state data)
      state)))

(def inc-or-0 (fnil inc 0))

(defn inc-check-count [state]
  (update state :check-count inc-or-0))

(defn add-previous-state [state previous-state]
  (update state :history (fnil conj (list)) (dissoc previous-state :history)))

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