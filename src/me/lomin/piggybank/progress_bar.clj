(ns me.lomin.piggybank.progress-bar
  (:require [progrock.core :as pr]))

(defn- make-progress-bar
  "Return a function which describe a progress bar"
  ([curr change-f max* partitions]
   (let [partition-size (int (/ max* partitions))]
     (fn bar
       ([] (bar :inc))
       ([action]
        (bar action 1))
       ([action by]
        (case action
          :inc (change-f curr pr/tick 1)
          :dec (change-f curr pr/tick -1)
          :complete (change-f pr/done)
          :reset (change-f curr (constantly 0)))
        (let [progress @curr]
          (when (or (= 1 (:progress progress))
                    (= max* (:progress progress))
                    (= (mod (:progress progress) partition-size) 0))
            (pr/print progress)))
        (when (< 1 by)
          (recur action (dec by))))))))

(defn make-blocking-progress-bar
  ([] (make-blocking-progress-bar {:max 100}))
  ([{max* :max partitions :partitions}]
   (make-progress-bar (atom (pr/progress-bar max*))
                      swap!
                      max*
                      (or partitions max*))))

(defn make-unblocking-progress-bar
  ([] (make-unblocking-progress-bar {:max 100}))
  ([{max* :max partitions :partitions}]
   (make-progress-bar (agent (pr/progress-bar max*))
                      send
                      max*
                      (or partitions max*))))

(defn make-fuzzy-progress-bar
  ([] (make-fuzzy-progress-bar {:max 100}))
  ([{max* :max partitions :partitions}]
   (make-progress-bar (volatile! (pr/progress-bar max*))
                      (fn ([store f]
                           (vswap! store f))
                        ([store f arg]
                         (vswap! store f arg)))
                      max*
                      (or partitions max*))))
