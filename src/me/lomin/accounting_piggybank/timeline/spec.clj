(ns me.lomin.accounting-piggybank.timeline.spec
  (:require [clojure.spec.alpha :as s]))

(defn infinite-seq-of
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

(s/def ::event (s/spec (s/cat :type keyword? :data (s/? map?))))
(s/def ::event-set (s/coll-of ::event :kind set?))
(s/def ::timeline (s/coll-of ::event :kind vector?))
(s/def ::timeline-set (s/coll-of ::timeline :kind set?))
(s/def ::timeline-context (s/keys :req-un [::timeline]))
(s/def ::model (s/fspec :args (s/cat :timeline-context ::timeline-context)
                        :ret ::event-set))
(s/def ::timeline-event-pair (s/cat :timeline ::timeline
                                    :event ::event))
(s/def ::timeline-event-pair-seq (s/every ::timeline-event-pair))