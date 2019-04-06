(ns me.lomin.accounting-piggybank.model.core
  (:require [clojure.math.combinatorics :as combo]
            [clojure.set :as set]
            [me.lomin.accounting-piggybank.model.logic :refer [for-all there-exists]]))

(defn get-event-id [[_ data]]
  (:id data))

(defn find-events [timeline event-type]
  (filter (fn [[event-type*]]
            (= event-type* event-type))
          timeline))

(defn get-last-id [timeline]
  (or (->> (find-events timeline :user)
           (sort-by get-event-id)
           (last)
           (get-event-id))
      -1))

(defn- get-next-id [timeline]
  (inc (get-last-id timeline)))

(defn- combine-events-with [f event-types]
  (fn [event-candidates {[_ data] :event}]
    (f event-candidates
       (set (map (fn [event-type] [event-type data])
                 event-types)))))

(defn- writing-process-finished? [timeline]
  (for-all [incoming-event (find-events timeline :user)]
           (there-exists [write-completion-event (find-events timeline :state-write)]
                         (= (get-event-id incoming-event)
                            (get-event-id write-completion-event)))))

(defn- generate-incoming-events-from [timeline events]
  (map (fn [event id]
         (update event 1 assoc :id id))
       events
       (iterate inc (get-next-id timeline))))

(defn- combine [event-candidates combinators context]
  (if combinators
    (combinators event-candidates context)
    event-candidates))

(defn init [model context]
  (into #{} (combine [#{}] (get model ::always) context)))

(defn flat-set [xs]
  (into #{} cat xs))

(defn make-model [model
                  {:keys [timeline] :as context}]
  (let [context* (assoc context :model model)]
    (flat-set (reduce (fn [event-candidates [event-type :as event]]
                        (combine event-candidates
                                 (get model event-type)
                                 (assoc context* :event event)))
                      (combine [#{}] (get model ::always) context*)
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

;; models

(def multi-threaded-simple-model
  (partial make-model
           {::always     (all (generate-incoming multi-threaded
                                                 [:user {:val 1}]
                                                 [:user {:val -1}])
                              (always [:stuttering]))
            :user        (all (triggers :db-read))
            :db-read     (all (triggers :db-write)
                              (prevents :db-read))
            :db-write    (all (triggers :state-write)
                              (prevents :db-write))
            :state-write (all (prevents :state-write))}))

(def single-threaded-simple-model
  (partial make-model
           {::always     (all (generate-incoming single-threaded
                                                 [:user {:val 1}]
                                                 [:user {:val -1}])
                              (always [:stuttering]))
            :user        (all (triggers :db-read)
                              (prevents :user))
            :db-read     (all (triggers :db-write)
                              (prevents :db-read))
            :db-write    (all (triggers :state-write)
                              (prevents :db-write))
            :state-write (all (prevents :state-write))}))

(def single-threaded+pagination-model
  (partial make-model
           {::always                 (all (generate-incoming single-threaded
                                                             [:user {:val 1}]
                                                             [:user {:val -1}])
                                          (always [:stuttering]))
            :user                    (all (triggers :db-read)
                                          (prevents :user))
            :db-read                 (all (triggers :db-write
                                                    :db-link-to-new-document
                                                    :db-add-new-document)
                                          (prevents :db-read))
            :db-write                (all (triggers :state-write)
                                          (prevents :db-write))
            :db-link-to-new-document (all (prevents :db-link-to-new-document))
            :db-add-new-document     (all (prevents :db-add-new-document))
            :state-write             (all (prevents :state-write))}))

(def single-threaded+safe-pagination-model
  (partial make-model
           {::always                 (all (generate-incoming multi-threaded
                                                             [:user {:val 1}]
                                                             [:user {:val -1}])
                                          (always [:stuttering]))
            :user                    (all (triggers :db-read)
                                          (prevents :user))
            :db-read                 (& (choose (&* (triggers :db-write)
                                                    (prevents :db-read))
                                                (&* (triggers :db-add-new-document)
                                                    (prevents :db-read)))
                                        (all (prevents :db-read)))
            :db-write                (all (only :state-write))
            :db-add-new-document     (all (only :db-link-to-new-document))
            :db-link-to-new-document (all (only :db-write))
            :state-write             (all (prevents :state-write))}))

(def model+safe-pagination+gc
  (partial make-model
           {::always                  (all (generate-incoming single-threaded
                                                              [:user {:val 1}]
                                                              [:user {:val -1}])
                                           (always [:stuttering]))
            :user                     (all (triggers :db-read)
                                           (prevents :user))
            :db-read                  (all (triggers :db-write
                                                     :db-add-new-document)
                                           (prevents :db-read))
            :db-write                 (all (triggers :state-write)
                                           (prevents :db-write))
            :db-link-to-new-document  (all (prevents :db-link-to-new-document))
            :db-add-new-document      (all (triggers :db-link-to-new-document)
                                           (prevents :db-add-new-document))
            :state-write              (all (triggers :db-gc-new-branch)
                                           (prevents :state-write))
            :db-gc-new-branch         (all (triggers :db-gc-link-to-new-branch)
                                           (prevents :db-gc-new-branch))
            :db-gc-link-to-new-branch (all (prevents :db-gc-link-to-new-branch))}))

(def model+safe-pagination+gc-strict
  (partial make-model
           {::always                  (all (generate-incoming single-threaded
                                                              [:user {:val 1}]
                                                              [:user {:val -1}])
                                           (always [:stuttering]))
            :user                     (all (triggers :db-read)
                                           (prevents :user))
            :db-read                  (& (choose (triggers :db-write)
                                                 (triggers :db-add-new-document))
                                         (all (prevents :db-read)))
            :db-write                 (all (only :state-write))
            :db-add-new-document      (all (only :db-link-to-new-document))
            :db-link-to-new-document  (all (only :db-write))
            :state-write              (all (only :db-gc-new-branch))
            :db-gc-new-branch         (all (only :db-gc-link-to-new-branch))
            :db-gc-link-to-new-branch (all (generate-incoming single-threaded
                                                              [:user {:val 1}]
                                                              [:user {:val -1}])
                                           (prevents :db-gc-link-to-new-branch))}))