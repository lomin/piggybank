(ns me.lomin.piggybank.checker
  (:require [clojure.core.reducers :as r]
            [clojure.data :as data]
            [clojure.spec.alpha :as s]
            [clojure.test :as test]
            [clojure.test :as clojure-test]
            [me.lomin.piggybank.progress-bar :as pgb]
            [me.lomin.piggybank.timeline :as timeline]))

(defn spec-invalid? [spec x]
  (test/is (not (s/valid? spec x))
           (if (s/valid? spec x)
             (s/explain spec x))))

(defn spec-valid? [spec x]
  (test/is (s/valid? spec x)
           (if (not (s/valid? spec x))
             (s/explain spec x))))

(defn =* [expected actual]
  (let [[things-only-in-expected _ things-in-both]
        (data/diff expected actual)]
    (if things-only-in-expected
      actual
      things-in-both)))

(defmethod clojure-test/assert-expr '=* [msg form]
  (let [[_ expected actual] form]
    (clojure-test/assert-expr msg
                              (list '=
                                    expected
                                    (list 'me.lomin.piggybank.checker/=*
                                          expected
                                          actual)))))

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
  ([{:keys [model length keys interpreter universe]}]
   (let [timelines (timeline/all-timelines-of-length length model)
         max-check-count (* length (count timelines))
         progress-bar (pgb/make-fuzzy-progress-bar max-check-count)
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
