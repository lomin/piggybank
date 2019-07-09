(ns me.lomin.piggybank.accounting.model.core
  (:require [me.lomin.piggybank.logic :refer [for-all there-exists]]
            [me.lomin.piggybank.model :refer [all
                                              START
                                              always
                                              choose
                                              triggers-for-every-past
                                              generate-incoming
                                              make-model
                                              multi-threaded
                                              only
                                              triggers] :as model]))

(defn- writing-process-finished? [timeline]
  (for-all [incoming-event (model/find-events timeline :process)]
           (there-exists [write-completion-event (model/find-events timeline :balance-write)]
                         (= (model/get-process-id incoming-event)
                            (model/get-process-id write-completion-event)))))

(defn single-threaded [{:keys [timeline]}]
  (writing-process-finished? timeline))

;; models

(def multi-threaded-simple-model
  (partial make-model
           {START             (all (generate-incoming multi-threaded
                                                      [:process {:amount 1}]
                                                      [:process {:amount -1}])
                                   (always [:stuttering]))
            :process          (all (triggers :accounting-read))
            :accounting-read  (all (triggers :accounting-write))
            :accounting-write (all (triggers :balance-write))
            :balance-write    (all (triggers))}))

(def single-threaded-simple-model
  (partial make-model
           {START             (all (generate-incoming single-threaded
                                                      [:process {:amount 1}]
                                                      [:process {:amount -1}])
                                   (always [:stuttering]))
            :process          (all (triggers :accounting-read))
            :accounting-read  (all (triggers :accounting-write))
            :accounting-write (all (triggers :balance-write))
            :balance-write    (all (triggers))}))

(def single-threaded+pagination-model
  (partial make-model
           {START                            (all (generate-incoming single-threaded
                                                                     [:process {:amount 1}]
                                                                     [:process {:amount -1}])
                                                  (always [:stuttering]))
            :process                         (all (triggers :accounting-read))
            :accounting-read                 (all (triggers :accounting-write
                                                            :accounting-link-to-new-document
                                                            :accounting-add-new-document))
            :accounting-write                (all (triggers :balance-write))
            :accounting-link-to-new-document (all (triggers))
            :accounting-add-new-document     (all (triggers))
            :balance-write                   (all (triggers))}))

(def single-threaded+safe-pagination-model
  (partial make-model
           {START                            (all (generate-incoming multi-threaded
                                                                     [:process {:amount 1}]
                                                                     [:process {:amount -1}])
                                                  (always [:stuttering]))
            :process                         (all (triggers :accounting-read))
            :accounting-read                 (choose (triggers :accounting-write)
                                                     (triggers :accounting-add-new-document))
            :accounting-write                (all (only :balance-write))
            :accounting-add-new-document     (all (only :accounting-link-to-new-document))
            :accounting-link-to-new-document (all (only :accounting-write))
            :balance-write                   (all (triggers))}))

(def model+safe-pagination+gc-strict
  (partial make-model
           {START                             (all (generate-incoming single-threaded
                                                                      [:process {:amount 1}]
                                                                      [:process {:amount -1}])
                                                   (always [:stuttering]))
            :process                          (all (triggers :accounting-read))
            :accounting-read                  (choose (triggers :accounting-write)
                                                      (triggers :accounting-add-new-document))
            :accounting-write                 (all (only :balance-write))
            :accounting-add-new-document      (all (only :accounting-link-to-new-document))
            :accounting-link-to-new-document  (all (only :accounting-write))
            :balance-write                    (all (only :accounting-gc-new-branch))
            :accounting-gc-new-branch         (all (only :accounting-gc-link-to-new-branch))
            :accounting-gc-link-to-new-branch (all (triggers))}))

(def single-threaded+inmemory-balance+eventually-consistent-accounting-model
  (partial make-model
           {START             (all (generate-incoming single-threaded
                                                      [:process {:amount 1}]
                                                      [:process {:amount -1}]))
            :restart          (all (only))
            :process          (choose (triggers-for-every-past :restart)
                                      (triggers :accounting-read))
            :accounting-read  (choose (triggers-for-every-past :restart)
                                      (triggers :accounting-write))
            :accounting-write (choose (triggers-for-every-past :restart)
                                      (triggers :balance-write))
            :balance-write    (choose (triggers-for-every-past :restart)
                                      (only))}))