(ns me.lomin.piggybank.model
  (:require [clojure.math.combinatorics :as combo]
            [clojure.set :as set]
            [me.lomin.piggybank.logic :refer [for-all there-exists]]))

(def ALWAYS ::always)

(defn get-process-id [[_ data]]
  (:process-id data))

(defn find-events [timeline event-type]
  (filter (fn [[event-type*]]
            (= event-type* event-type))
          timeline))

(defn get-last-process-id [timeline]
  (or (->> (find-events timeline :process)
           (sort-by get-process-id)
           (last)
           (get-process-id))
      -1))

(defn- get-next-process-id [timeline]
  (inc (get-last-process-id timeline)))

(defn- combine-events-with [f event-types]
  (fn [event-candidates {[_ data] :event}]
    (f event-candidates
       (set (map (fn [event-type] [event-type data])
                 event-types)))))

(defn- writing-process-finished? [timeline]
  (for-all [incoming-event (find-events timeline :process)]
           (there-exists [write-completion-event (find-events timeline :balance-write)]
                         (= (get-process-id incoming-event)
                            (get-process-id write-completion-event)))))

(defn- generate-incoming-events-from [timeline events]
  (map (fn [event id]
         (update event 1 assoc :process-id id))
       events
       (iterate inc (get-next-process-id timeline))))

(defn- combine [event-candidates combinators context]
  (if combinators
    (combinators event-candidates context)
    event-candidates))

(defn init [model context]
  (into #{} (combine [#{}] (get model ALWAYS) context)))

(defn flat-set [xs]
  (into #{} cat xs))

(defn make-model [model
                  {:keys [timeline] :as context}]
  (let [context* (assoc context :model model)]
    (flat-set (reduce (fn [event-candidates [event-type :as event]]
                        (combine event-candidates
                                 (get model event-type)
                                 (assoc context* :event event)))
                      (combine [#{}] (get model ALWAYS) context*)
                      timeline))))

;; combinators

(defn single-threaded [{:keys [timeline]}]
  (writing-process-finished? timeline))

(defn multi-threaded [_] true)

(defn always [& events]
  (fn [event-candidates _]
    (set/union event-candidates (set events))))

(defn generate-incoming [pred & events]
  (fn [event-candidates {:keys [timeline] :as context}]
    (set/union event-candidates
               (set (when (pred context)
                      (generate-incoming-events-from timeline events))))))

(defn for-every-past [& events]
  (fn [event-candidates {:keys [timeline]}]
    (set/union event-candidates
               (set (map (fn [[event i]] [event {:past i}])
                         (combo/cartesian-product events
                                                  (range (inc (count timeline)))))))))

(defn triggers [& event-types]
  (combine-events-with set/union event-types))

(defn prevents [& event-types]
  (combine-events-with set/difference event-types))

(defn only [& allowed-event-types]
  (fn [_ {model :model :as context}]
    (let [f (apply triggers allowed-event-types)]
      (f (flat-set (init model context))
         context))))

(defn &* [& low-level-combinators]
  (fn [event-candidates context]
    (let [f (apply comp
                   (map #(fn [event-candidates-set*]
                           (% event-candidates-set* context))
                        low-level-combinators))]
      (f event-candidates))))

;; top-level combinators

(defn all [& combinators]
  (fn [event-candidates-set context]
    (let [f (apply &* combinators)]
      (map (fn [event-candidates] (f event-candidates context))
           event-candidates-set))))

(defn choose [& combinators]
  (fn [event-candidates-set context]
    (into #{}
          (map (fn [[event-candidates f]] (f event-candidates context))
               (combo/cartesian-product event-candidates-set
                                        combinators)))))

;; combinator of combinators

(defn & [& top-level-combinators]
  (fn [event-candidates context]
    (reduce (fn [event-candidates* combinator]
              (combinator event-candidates* context))
            event-candidates
            top-level-combinators)))