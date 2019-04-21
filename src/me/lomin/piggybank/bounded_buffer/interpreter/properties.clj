(ns me.lomin.piggybank.bounded-buffer.interpreter.properties
  (:require [me.lomin.piggybank.bounded-buffer.spec :as spec]
            [me.lomin.piggybank.timeline :as timeline]))

(defn keep-essential-keys [universe]
  (select-keys universe (keys spec/empty-universe)))

(defn deadlock [universe event model interpret-event]
  (let [successor-events (mapv second
                               (timeline/successor-timelines [model [event]]))]
    (every? #(= (keep-essential-keys %) (keep-essential-keys universe))
            (for [event successor-events]
              (interpret-event universe event)))))

(defn any-property-violation [{:keys [universe model event interpret-event] :as context}]
  (cond
    (deadlock universe event model interpret-event) :there-must-be-no-deadlocks))