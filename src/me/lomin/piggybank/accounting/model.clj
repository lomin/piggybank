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
                                              single-threaded
                                              START
                                              then
                                              then-for-every-past-time-slot] :as model]))
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
   {START                   (all (generate-incoming multi-threaded
                                                    [:process {:amount 1}]
                                                    [:process {:amount -1}]))
    :process                 (all (then :balance-read))
    :balance-read            (all (then :accounting-read))
    :accounting-read         (all (then :accounting-write))
    :accounting-write        (all (then :terminate/balance-write))
    :terminate/balance-write (continue)}))

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
   {START                   (all (generate-incoming single-threaded
                                                    [:process {:amount 1}]
                                                    [:process {:amount -1}]))
    :process                 (all (then :balance-read))
    :balance-read            (all (then :accounting-read))
    :accounting-read         (all (then :accounting-write))
    :accounting-write        (all (then :terminate/balance-write))
    :terminate/balance-write (continue)}))

(def single-threaded+pagination-model
  (make-model
   {START                           (all (generate-incoming single-threaded
                                                            [:process {:amount 1}]
                                                            [:process {:amount -1}])
                                         (always [:stuttering]))
    :process                         (all (then :balance-read))
    :balance-read                    (all (then :accounting-read))
    :accounting-read                 (all (then :accounting-write
                                                :accounting-link-to-new-document
                                                :accounting-add-new-document))
    :accounting-write                (all (then :terminate/balance-write))
    :accounting-link-to-new-document (continue)
    :accounting-add-new-document     (continue)
    :terminate/balance-write         (continue)}))

(def single-threaded+safe-pagination-model
  (make-model
   {START                           (all (generate-incoming multi-threaded
                                                            [:process {:amount 1}]
                                                            [:process {:amount -1}])
                                         (always [:stuttering]))
    :process                         (all (then :balance-read))
    :balance-read                    (all (then :accounting-read))
    :accounting-read                 (choose (then :accounting-write)
                                             (then :accounting-add-new-document))
    :accounting-write                (all (only :terminate/balance-write))
    :accounting-add-new-document     (all (only :accounting-link-to-new-document))
    :accounting-link-to-new-document (all (only :accounting-write))
    :terminate/balance-write         (continue)}))

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
    :accounting-write                 (all (only :terminate/balance-write))
    :accounting-add-new-document      (all (only :accounting-link-to-new-document))
    :accounting-link-to-new-document  (all (only :accounting-write))
    :terminate/balance-write          (all (only :accounting-gc-new-branch))
    :accounting-gc-new-branch         (all (only :accounting-gc-link-to-new-branch))
    :accounting-gc-link-to-new-branch (continue)}))

(def single-threaded+inmemory-balance+eventually-consistent-accounting-model
  (make-model
   {START                       (all (generate-incoming single-threaded
                                                        [:process {:amount 1}]
                                                        [:process {:amount -1}]))
    :terminate/restart          (all (only))
    :process                    (choose (then-for-every-past-time-slot :terminate/restart)
                                        (then :balance-read))
    :balance-read               (choose (then-for-every-past-time-slot :terminate/restart)
                                        (then :accounting-read))
    :accounting-read            (choose (then-for-every-past-time-slot :terminate/restart)
                                        (then :accounting-write))
    :accounting-write           (choose (then-for-every-past-time-slot :terminate/restart)
                                        (then :accounting-read-last-write))
    :accounting-read-last-write (choose (then-for-every-past-time-slot :terminate/restart)
                                        (then :terminate/balance-write))
    :terminate/balance-write    (choose (then-for-every-past-time-slot :terminate/restart)
                                        (only))}))