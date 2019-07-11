(ns me.lomin.piggybank.model
  (:require [clojure.math.combinatorics :as combo]
            [clojure.set :as set]
            [me.lomin.piggybank.logic :refer [for-all there-exists]]))

(def START ::start)

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

(defn- make-event-candidates-default [event-types {[_ data] :event}]
  (set (map (fn [event-type] [event-type data])
            event-types)))

(defn combine-events-with [combine-f make-event-candidates event-types]
  (fn [event-candidates {[e-type] :event :as context}]
    (set/difference (combine-f event-candidates
                               (make-event-candidates event-types context))
                    (make-event-candidates-default (clojure.set/difference #{e-type}
                                                                           (set event-types))
                                                   context))))

(defn generate-incoming-events-from [timeline events]
  (map (fn [event id]
         (update event 1 assoc :process-id id))
       events
       (repeat (get-next-process-id timeline))))

(defn- combine [event-candidates combinators context]
  (if combinators
    (combinators event-candidates context)
    event-candidates))

(defn init [model context]
  (into #{} (combine [#{}] (get model START) context)))

(defn flat-set [xs]
  (into #{} cat xs))

(defn make-model [model
                  {:keys [timeline] :as context}]
  (let [context* (assoc context :model model)]
    (flat-set (reduce (fn [event-candidates [event-type :as event]]
                        (combine event-candidates
                                 (get model event-type)
                                 (assoc context* :event event)))
                      (combine [#{}] (get model START) context*)
                      timeline))))

;; combinators

(defn multi-threaded [_] true)

(defn always [& events]
  (fn [event-candidates _]
    (set/union event-candidates (set events))))

(defn generate-incoming [pred & events]
  (fn [event-candidates {:keys [timeline] :as context}]
    (set/union event-candidates
               (set (when (pred context)
                      (generate-incoming-events-from timeline events))))))

(defn prevents [& event-types]
  (combine-events-with set/difference
                       make-event-candidates-default
                       event-types))

(defn- &* [& low-level-combinators]
  (fn [event-candidates context]
    (let [f (apply comp
                   (map #(fn [event-candidates-set*]
                           (% event-candidates-set* context))
                        low-level-combinators))]
      (f event-candidates))))

(defn then [& event-types]
  (combine-events-with set/union
                       make-event-candidates-default
                       event-types))

(defn only [& allowed-event-types]
  (fn [_ {model :model :as context}]
    (let [f (apply then allowed-event-types)]
      (f (flat-set (init model context))
         context))))

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

(defn continue [] (all (then)))
(defn restart [] (only))

;; combinator of combinators

(defn & [& top-level-combinators]
  (fn [event-candidates context]
    (reduce (fn [event-candidates* combinator]
              (combinator event-candidates* context))
            event-candidates
            top-level-combinators)))