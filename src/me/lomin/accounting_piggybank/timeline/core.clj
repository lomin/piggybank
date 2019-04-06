(ns me.lomin.accounting-piggybank.timeline.core
  (:require [me.lomin.accounting-piggybank.timeline.spec :as s]
            [me.lomin.sayang :refer [of sdefn]]))

(def EMPTY-TIMELINES #{[]})

(defn flat-set [& xforms]
  #(into #{} (comp (apply comp xforms) cat) %))

(defn pair-with [x]
  (fn [y] [x y]))

(sdefn multiply {:ret ::s/timeline-set}
       [[timeline :- ::s/timeline]
        [event-set :- ::s/event-set]]
       (set (map conj
                 (repeat timeline)
                 event-set)))

(sdefn successor-timelines {:ret ::s/timeline-set}
       [[model timeline]]
       (multiply timeline (model {:timeline timeline})))

(sdefn infinite-timelines-seq {:ret (s/infinite-seq-of ::s/timeline-set)}
       [[model :- ::s/model]
        [start-timelines :- ::s/timeline-set]]
       (iterate (flat-set (map (pair-with model))
                          (map successor-timelines))
                start-timelines))

(sdefn all-timelines-of-length {:ret ::s/timeline-set}
       [[length :- int?]
        [model :- ::s/model]]
       (nth (infinite-timelines-seq model EMPTY-TIMELINES)
            length))