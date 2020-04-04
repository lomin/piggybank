(ns me.lomin.piggybank.checker
  (:require [clojure.core.reducers :as r]
            [me.lomin.piggybank.progress-bar :as pgb]
            [me.lomin.piggybank.timeline :as timeline]))

(def plus (fnil + 0 0))

(defn dereduce [x]
  (if (reduced? x)
    @x
    x))

(defn merge-check-results [check-result-0 check-result-1]
  (let [result (-> check-result-0
                   (merge check-result-1)
                   (update :check-count plus (:check-count check-result-0)))]
    (if (:property-violated result)
      (reduced result)
      result)))

(defn check-properties [universe]
  (fn
    ([] universe)
    ([state] state)
    ([state-0 state-1]
     (let [state-0* (dereduce state-0)
           state-1* (dereduce state-1)]
       (if (:property-violated state-1*)
         (merge-check-results state-0* state-1*)
         (merge-check-results state-1* state-0*))))))

(defn check
  ([{:keys [model length keys interpreter universe partitions prelines]}]
   (let [prelines (vec (or (seq prelines) []))
         timelines (timeline/all-timelines-of-length length model #{prelines})
         max-check-count (* (+ length (count prelines)) (count timelines))
         progress-bar (pgb/make-fuzzy-progress-bar {:max        max-check-count
                                                    :partitions partitions})
         result (-> (r/fold (check-properties universe)
                            (r/map (comp interpreter
                                         (fn [timeline]
                                           {:progress-bar progress-bar
                                            :universe     universe
                                            :model        model
                                            :timeline     timeline}))
                                   (vec timelines)))
                    (dereduce)
                    (assoc :max-check-count max-check-count))]
     (if keys
       (select-keys result keys)
       result))))