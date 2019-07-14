(ns me.lomin.piggybank.bounded-buffer.model
  (:require [clojure.math.combinatorics :as combo])
  (:require [me.lomin.piggybank.logic :refer [for-all there-exists]]
            [me.lomin.piggybank.model :refer [all
                                              always
                                              make-model
                                              START]]))

(def simple-bounded-buffer-model
  (partial make-model
           {START (all (always [:consumer {:id 0}])
                       (always [:consumer {:id 1}])
                       (always [:producer {:id 2}]))}))

(def remove-obvious-wrong-models
  (remove #(apply = %)))

(def assign-ids-to-events
  (map #(map-indexed (fn [index t] [t {:id index}]) %)))

(defn assign-notify-ids-to-events [number-of-threads]
  (map #(map (fn [[event notify-id]]
               (update event 1 assoc :notify notify-id))
             (combo/cartesian-product % (range number-of-threads)))))

(defn all-models [number-of-threads]
  (sequence (comp (map sort)
                  (distinct)
                  remove-obvious-wrong-models
                  assign-ids-to-events
                  (assign-notify-ids-to-events number-of-threads))
            (combo/selections #{:consumer :producer} number-of-threads)))

(def two-threads-model
  (make-model
   {START (all (always [:consumer {:id 0, :notify 0}])
               (always [:consumer {:id 0, :notify 1}])
               (always [:producer {:id 1, :notify 0}])
               (always [:producer {:id 1, :notify 1}]))}))

(def two-producers-model
  (make-model
   {START (all (always [:consumer {:id 0, :notify 0}])
               (always [:consumer {:id 0, :notify 1}])
               (always [:consumer {:id 0, :notify 2}])
               (always [:producer {:id 1, :notify 0}])
               (always [:producer {:id 1, :notify 1}])
               (always [:producer {:id 1, :notify 2}])
               (always [:producer {:id 2, :notify 0}])
               (always [:producer {:id 2, :notify 1}])
               (always [:producer {:id 2, :notify 2}]))}))

(def extreme-programming-challenge-fourteen
  (make-model
   {START (all (always [:consumer {:id 0, :notify 0}])
               (always [:consumer {:id 0, :notify 1}])
               (always [:consumer {:id 0, :notify 2}])
               (always [:consumer {:id 1, :notify 0}])
               (always [:consumer {:id 1, :notify 1}])
               (always [:consumer {:id 1, :notify 2}])
               (always [:producer {:id 2, :notify 0}])
               (always [:producer {:id 2, :notify 1}])
               (always [:producer {:id 2, :notify 2}]))}))
