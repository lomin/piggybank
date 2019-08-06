(ns me.lomin.piggybank.timeline
  (:require [clojure.spec.alpha :as s]
            [me.lomin.sayang :refer [sdefn]]))

;; spec

(defn infinite-seq-of
  ;; s/*coll-check-limit* has to bound in every function.
  ;; Binding it once above (reify ...) has no effect.
  ([s] (infinite-seq-of s 3))
  ([s coll-check-limit]
   (let [every* (s/every s)]
     (reify s/Spec
       (conform* [_ x] (binding [s/*coll-check-limit* coll-check-limit]
                         (s/conform* every* x)))
       (unform* [_ y] (binding [s/*coll-check-limit* coll-check-limit]
                        (s/unform* every* y)))
       (explain* [_ path via in x] (binding [s/*coll-check-limit* coll-check-limit]
                                     (s/explain* every* path via in x)))
       (gen* [_ overrides path rmap] (binding [s/*coll-check-limit* coll-check-limit]
                                       (s/gen* every* overrides path rmap)))
       (with-gen* [_ gfn] (binding [s/*coll-check-limit* coll-check-limit]
                            (s/with-gen* every* gfn)))
       (describe* [_] (binding [s/*coll-check-limit* coll-check-limit]
                        (s/describe* every*)))))))

(s/def ::event (s/spec (s/cat :type keyword? :transfers (s/? map?))))
(s/def ::event-set (s/coll-of ::event :kind set?))
(s/def ::timeline (s/coll-of ::event :kind vector?))
(s/def ::timeline-set (s/coll-of ::timeline :kind set?))
(s/def ::timeline-context (s/keys :req-un [::timeline]))
(s/def ::model (s/fspec :args (s/cat :timeline-context ::timeline-context)
                        :ret ::event-set))
(s/def ::timeline-event-pair (s/cat :timeline ::timeline
                                    :event ::event))
(s/def ::timeline-event-pair-seq (s/every ::timeline-event-pair))

;; impl

(def EMPTY-TIMELINES #{[]})

(defn flat-set [& xforms]
  #(into #{} (comp (apply comp xforms) cat) %))

(defn pair-with [x]
  (fn [y] [x y]))

(sdefn multiply {:ret ::timeline-set}
       [[timeline :- ::timeline]
        [event-set :- ::event-set]]
       (set (map conj
                 (repeat timeline)
                 event-set)))

(sdefn successor-timelines {:ret ::timeline-set}
       [[model timeline]]
       (multiply timeline (model {:timeline timeline})))

(sdefn infinite-timelines-seq {:ret (infinite-seq-of ::timeline-set)}
       [[model :- ::model]
        [start-timelines :- ::timeline-set]]
       (iterate (flat-set (map (pair-with model))
                          (map successor-timelines))
                start-timelines))

(sdefn all-timelines-of-length {:ret ::timeline-set}
       ([[length :- int?]
         [model :- ::model]]
        (all-timelines-of-length length model EMPTY-TIMELINES))
       ([[length :- int?]
         [model :- ::model]
         [init-timelines :- ::timeline-set]]
        (nth (infinite-timelines-seq model init-timelines)
             length)))