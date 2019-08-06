(ns me.lomin.piggybank.checker
  (:require [clojure.core.reducers :as r]
            [com.rpl.specter :as s]
            [me.lomin.piggybank.progress-bar :as pgb]
            [me.lomin.piggybank.timeline :as timeline]))

(def plus (fnil + 0 0))

(defn check-properties [universe]
  (fn
    ([] universe)
    ([state] state)
    ([state-0 state-1]
     (let [state-1* (update state-1 :check-count plus (:check-count state-0))]
       (if (:property-violated state-1)
         (reduced state-1*)
         state-1*)))))

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
                                   timelines))
                    (assoc :max-check-count max-check-count))]
     (if keys
       (select-keys result keys)
       result))))