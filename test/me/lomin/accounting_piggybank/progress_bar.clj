(ns me.lomin.accounting-piggybank.progress-bar
  (:require [progrock.core :as pr]))

(defn- make-progress-bar
  "Return a function which describe a progress bar"
  ([curr change-f]
   (fn bar
     ([] (bar :inc))
     ([action]
      (bar action 1))
     ([action by]
      (case action
        :inc (change-f curr pr/tick by)
        :dec (change-f curr pr/tick (* by -1))
        :complete (change-f pr/done)
        :reset (change-f curr (constantly 0)))
      (pr/print @curr)))))

(defn make-blocking-progress-bar
  ([] (make-blocking-progress-bar 100))
  ([max*] (make-progress-bar (atom (pr/progress-bar max*))
                             swap!)))

(defn make-unblocking-progress-bar
  ([] (make-unblocking-progress-bar 100))
  ([max*] (make-progress-bar (agent (pr/progress-bar max*))
                             send)))

(defn make-fuzzy-progress-bar
  ([] (make-fuzzy-progress-bar 100))
  ([max*] (make-progress-bar (volatile! (pr/progress-bar max*))
                             (fn ([store f]
                                  (vswap! store f))
                               ([store f arg]
                                (vswap! store f arg))))))
