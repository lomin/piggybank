(ns me.lomin.piggybank.accounting.model
  (:require [clojure.math.combinatorics :as combo]
            [clojure.set :as set]
            [me.lomin.piggybank.logic :refer [for-all there-exists]]
            [me.lomin.piggybank.model :refer [all
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

(defn- make-event-candidates-for-every-past-time-slot [event-types {timeline :timeline}]
  (set (map (fn [[event i]] [event {:past i}])
            (combo/cartesian-product event-types
                                     (range (inc (count timeline)))))))

(defn then-for-every-past-time-slot [& event-types]
  (model/combine-events-with set/union
                             make-event-candidates-for-every-past-time-slot
                             event-types))

(defn- writing-process-finished? [timeline]
  (for-all [incoming-event (model/find-events timeline :process)]
           (there-exists [write-completion-event (model/find-events timeline :balance-write)]
                         (= (model/get-process-id incoming-event)
                            (model/get-process-id write-completion-event)))))

(defn single-threaded [{:keys [timeline]}]
  (writing-process-finished? timeline))

;; models

(def example-model-0
  (partial make-model
           {START             (all (generate-incoming multi-threaded
                                                      [:process {:amount 1}]
                                                      [:process {:amount -1}]))
            :process          (continue)}))

(def example-model-1
  (partial make-model
           {START             (all (generate-incoming multi-threaded
                                                      [:process {:amount 1}]
                                                      [:process {:amount -1}]))
            :process          (all (then :accounting-write))
            :accounting-write (continue)}))

(def multi-threaded-simple-model
  (partial make-model
           {START             (all (generate-incoming multi-threaded
                                                      [:process {:amount 1}]
                                                      [:process {:amount -1}])
                                   (always [:stuttering]))
            :process          (all (then :accounting-read))
            :accounting-read  (all (then :accounting-write))
            :accounting-write (all (then :balance-write))
            :balance-write    (continue)}))

(def single-threaded-simple-model
  (partial make-model
           {START             (all (generate-incoming single-threaded
                                                      [:process {:amount 1}]
                                                      [:process {:amount -1}])
                                   (always [:stuttering]))
            :process          (all (then :accounting-read))
            :accounting-read  (all (then :accounting-write))
            :accounting-write (all (then :balance-write))
            :balance-write    (continue)}))

(def single-threaded+pagination-model
  (partial make-model
           {START                            (all (generate-incoming single-threaded
                                                                     [:process {:amount 1}]
                                                                     [:process {:amount -1}])
                                                  (always [:stuttering]))
            :process                         (all (then :accounting-read))
            :accounting-read                 (all (then :accounting-write
                                                        :accounting-link-to-new-document
                                                        :accounting-add-new-document))
            :accounting-write                (all (then :balance-write))
            :accounting-link-to-new-document (continue)
            :accounting-add-new-document     (continue)
            :balance-write                   (continue)}))

(def single-threaded+safe-pagination-model
  (partial make-model
           {START                            (all (generate-incoming multi-threaded
                                                                     [:process {:amount 1}]
                                                                     [:process {:amount -1}])
                                                  (always [:stuttering]))
            :process                         (all (then :accounting-read))
            :accounting-read                 (choose (then :accounting-write)
                                                     (then :accounting-add-new-document))
            :accounting-write                (all (only :balance-write))
            :accounting-add-new-document     (all (only :accounting-link-to-new-document))
            :accounting-link-to-new-document (all (only :accounting-write))
            :balance-write                   (continue)}))

(def model+safe-pagination+gc-strict
  (partial make-model
           {START                             (all (generate-incoming single-threaded
                                                                      [:process {:amount 1}]
                                                                      [:process {:amount -1}])
                                                   (always [:stuttering]))
            :process                          (all (then :accounting-read))
            :accounting-read                  (choose (then :accounting-write)
                                                      (then :accounting-add-new-document))
            :accounting-write                 (all (only :balance-write))
            :accounting-add-new-document      (all (only :accounting-link-to-new-document))
            :accounting-link-to-new-document  (all (only :accounting-write))
            :balance-write                    (all (only :accounting-gc-new-branch))
            :accounting-gc-new-branch         (all (only :accounting-gc-link-to-new-branch))
            :accounting-gc-link-to-new-branch (continue)}))

(def single-threaded+inmemory-balance+eventually-consistent-accounting-model
  (partial make-model
           {START             (all (generate-incoming single-threaded
                                                      [:process {:amount 1}]
                                                      [:process {:amount -1}]))
            :restart          (all (restart))
            :process          (choose (then-for-every-past-time-slot :restart)
                                      (then :accounting-read))
            :accounting-read  (choose (then-for-every-past-time-slot :restart)
                                      (then :accounting-write))
            :accounting-write (choose (then-for-every-past-time-slot :restart)
                                      (then :balance-write))
            :balance-write    (choose (then-for-every-past-time-slot :restart)
                                      (restart))}))