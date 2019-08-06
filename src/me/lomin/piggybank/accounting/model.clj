(ns me.lomin.piggybank.accounting.model
  (:require [clojure.math.combinatorics :as combo]
            [clojure.set :as set]
            [me.lomin.piggybank.logic :refer [for-all there-exists]]
            [me.lomin.piggybank.model :refer [&
                                              all
                                              always
                                              choose
                                              continue
                                              generate-incoming
                                              make-model
                                              multi-threaded
                                              only
                                              restart
                                              START
                                              then] :as model]))

(defn n-back-in-time [up-to timeline]
  (comment range (- (inc (count timeline))
                    (-> (filter (fn [[_ event]] (= event up-to))
                                (map-indexed (fn [i [event]] [i event]) timeline))
                        (last)
                        (first)
                        (or -1)
                        (inc))))
  (range 2))

(defn find-restartable-process-id [timeline]
  (there-exists [[_ {pid :process-id}] (model/find-events timeline :process)]
                (and (for-all [[_ {restart-pid :process-id}] (model/find-events timeline :restart)]
                              (not= pid restart-pid))
                     (not (there-exists [[_ {finished-pid :process-id}] (model/find-events timeline :process)]
                                        (< pid finished-pid)))
                     pid)))

(defn- make-event-candidates-for-every-past-time-slot [event-types {timeline :timeline}]
  (let [result-pid (find-restartable-process-id timeline)]
    (set (map (fn [[event i]] [event {:go-steps-back-in-timeline i :process-id result-pid}])
              (combo/cartesian-product event-types
                                       (n-back-in-time nil timeline))))))

(defn then-for-every-past-time-slot [& event-types]
  (model/combine-events-with set/union
                             make-event-candidates-for-every-past-time-slot
                             event-types))

(defn- writing-process-finished? [timeline]
  (for-all [incoming-event (model/find-events timeline :process)]
           (or (there-exists [write-completion-event (model/find-events timeline :balance-write)]
                             (= (model/get-process-id incoming-event)
                                (model/get-process-id write-completion-event)))
               (there-exists [write-completion-event (model/find-events timeline :restart)]
                             (= (model/get-process-id incoming-event)
                                (model/get-process-id write-completion-event))))))

(defn single-threaded [{:keys [timeline]}]
  (writing-process-finished? timeline))

;; models

(def example-model-0
  (make-model
   {START    (all (generate-incoming multi-threaded
                                     [:process {:amount 1}]
                                     [:process {:amount -1}]))
    :process (continue)}))

(def example-model-1
  (make-model
   {START             (all (generate-incoming multi-threaded
                                              [:process {:amount 1}]
                                              [:process {:amount -1}]))
    :process          (all (then :accounting-write))
    :accounting-write (continue)}))

(def multi-threaded-simple-model
  (make-model
   {START             (all (generate-incoming multi-threaded
                                              [:process {:amount 1}]
                                              [:process {:amount -1}]))
    :process          (all (then :balance-read))
    :balance-read     (all (then :accounting-read))
    :accounting-read  (all (then :accounting-write))
    :accounting-write (all (then :balance-write))
    :balance-write    (continue)}))

(def reduced-multi-threaded-simple-model
  (make-model
   {START             (all (generate-incoming multi-threaded
                                              [:process {:amount 1}]))
    :process          (all (then :accounting-read))
    :accounting-read  (all (then :accounting-write))
    :accounting-write (continue)}))

(def reduced-single-threaded-simple-model
  (make-model
   {START             (all (generate-incoming single-threaded
                                              [:process {:amount 1}]))
    :process          (all (then :accounting-read))
    :accounting-read  (all (then :accounting-write))
    :accounting-write (continue)}))

(def single-threaded-simple-model
  (make-model
   {START             (all (generate-incoming single-threaded
                                              [:process {:amount 1}]
                                              [:process {:amount -1}]))
    :process          (all (then :balance-read))
    :balance-read     (all (then :accounting-read))
    :accounting-read  (all (then :accounting-write))
    :accounting-write (all (then :balance-write))
    :balance-write    (continue)}))

(def single-threaded+pagination-model
  (make-model
   {START                            (all (generate-incoming single-threaded
                                                             [:process {:amount 1}]
                                                             [:process {:amount -1}])
                                          (always [:stuttering]))
    :process                         (all (then :balance-read))
    :balance-read                    (all (then :accounting-read))
    :accounting-read                 (all (then :accounting-write
                                                :accounting-link-to-new-document
                                                :accounting-add-new-document))
    :accounting-write                (all (then :balance-write))
    :accounting-link-to-new-document (continue)
    :accounting-add-new-document     (continue)
    :balance-write                   (continue)}))

(def single-threaded+safe-pagination-model
  (make-model
   {START                            (all (generate-incoming multi-threaded
                                                             [:process {:amount 1}]
                                                             [:process {:amount -1}])
                                          (always [:stuttering]))
    :process                         (all (then :balance-read))
    :balance-read                    (all (then :accounting-read))
    :accounting-read                 (choose (then :accounting-write)
                                             (then :accounting-add-new-document))
    :accounting-write                (all (only :balance-write))
    :accounting-add-new-document     (all (only :accounting-link-to-new-document))
    :accounting-link-to-new-document (all (only :accounting-write))
    :balance-write                   (continue)}))

(def model+safe-pagination+gc-strict
  (make-model
   {START                             (all (generate-incoming single-threaded
                                                              [:process {:amount 1}]
                                                              [:process {:amount -1}])
                                           (always [:stuttering]))
    :process                          (all (then :balance-read))
    :balance-read                     (all (then :accounting-read))
    :accounting-read                  (choose (then :accounting-write)
                                              (then :accounting-add-new-document))
    :accounting-write                 (all (only :balance-write))
    :accounting-add-new-document      (all (only :accounting-link-to-new-document))
    :accounting-link-to-new-document  (all (only :accounting-write))
    :balance-write                    (all (only :accounting-gc-new-branch))
    :accounting-gc-new-branch         (all (only :accounting-gc-link-to-new-branch))
    :accounting-gc-link-to-new-branch (continue)}))

(def single-threaded+inmemory-balance+eventually-consistent-accounting-model
  (make-model
   {START                       (all (generate-incoming single-threaded
                                                        [:process {:amount 1}]
                                                        [:process {:amount -1}]))
    :restart                    (all (only))
    :process                    (choose (then-for-every-past-time-slot :restart)
                                        (then :balance-read))
    :balance-read               (choose (then-for-every-past-time-slot :restart)
                                        (then :accounting-read))
    :accounting-read            (choose (then-for-every-past-time-slot :restart)
                                        (then :accounting-write))
    :accounting-write           (choose (then-for-every-past-time-slot :restart)
                                        (then :accounting-read-last-write))
    :accounting-read-last-write (choose (then-for-every-past-time-slot :restart)
                                        (then :balance-write))
    :balance-write              (choose (then-for-every-past-time-slot :restart)
                                        (only))}))