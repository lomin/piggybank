(ns me.lomin.piggybank.accounting.model.core
  (:require [me.lomin.piggybank.logic :refer [for-all there-exists]]
            [me.lomin.piggybank.model :refer [&
                                              &*
                                              all
                                              ALWAYS
                                              always
                                              choose
                                              for-every-past
                                              generate-incoming
                                              make-model
                                              multi-threaded
                                              only
                                              prevents
                                              single-threaded
                                              triggers]]))

;; models

(def multi-threaded-simple-model
  (partial make-model
           {ALWAYS            (all (generate-incoming multi-threaded
                                                      [:process {:amount 1}]
                                                      [:process {:amount -1}])
                                   (always [:stuttering]))
            :process          (all (triggers :accounting-read))
            :accounting-read  (all (triggers :accounting-write)
                                   (prevents :accounting-read))
            :accounting-write (all (triggers :balance-write)
                                   (prevents :accounting-write))
            :balance-write    (all (prevents :balance-write))}))

(def single-threaded-simple-model
  (partial make-model
           {ALWAYS            (all (generate-incoming single-threaded
                                                      [:process {:amount 1}]
                                                      [:process {:amount -1}])
                                   (always [:stuttering]))
            :process          (all (triggers :accounting-read)
                                   (prevents :process))
            :accounting-read  (all (triggers :accounting-write)
                                   (prevents :accounting-read))
            :accounting-write (all (triggers :balance-write)
                                   (prevents :accounting-write))
            :balance-write    (all (prevents :balance-write))}))

(def single-threaded+pagination-model
  (partial make-model
           {ALWAYS                           (all (generate-incoming single-threaded
                                                                     [:process {:amount 1}]
                                                                     [:process {:amount -1}])
                                                  (always [:stuttering]))
            :process                         (all (triggers :accounting-read)
                                                  (prevents :process))
            :accounting-read                 (all (triggers :accounting-write
                                                            :accounting-link-to-new-document
                                                            :accounting-add-new-document)
                                                  (prevents :accounting-read))
            :accounting-write                (all (triggers :balance-write)
                                                  (prevents :accounting-write))
            :accounting-link-to-new-document (all (prevents :accounting-link-to-new-document))
            :accounting-add-new-document     (all (prevents :accounting-add-new-document))
            :balance-write                   (all (prevents :balance-write))}))

(def single-threaded+safe-pagination-model
  (partial make-model
           {ALWAYS                           (all (generate-incoming multi-threaded
                                                                     [:process {:amount 1}]
                                                                     [:process {:amount -1}])
                                                  (always [:stuttering]))
            :process                         (all (triggers :accounting-read)
                                                  (prevents :process))
            :accounting-read                 (& (choose (&* (triggers :accounting-write)
                                                            (prevents :accounting-read))
                                                        (&* (triggers :accounting-add-new-document)
                                                            (prevents :accounting-read)))
                                                (all (prevents :accounting-read)))
            :accounting-write                (all (only :balance-write))
            :accounting-add-new-document     (all (only :accounting-link-to-new-document))
            :accounting-link-to-new-document (all (only :accounting-write))
            :balance-write                   (all (prevents :balance-write))}))

(def model+safe-pagination+gc-strict
  (partial make-model
           {ALWAYS                            (all (generate-incoming single-threaded
                                                                      [:process {:amount 1}]
                                                                      [:process {:amount -1}])
                                                   (always [:stuttering]))
            :process                          (all (triggers :accounting-read)
                                                   (prevents :process))
            :accounting-read                  (& (choose (triggers :accounting-write)
                                                         (triggers :accounting-add-new-document))
                                                 (all (prevents :accounting-read)))
            :accounting-write                 (all (only :balance-write))
            :accounting-add-new-document      (all (only :accounting-link-to-new-document))
            :accounting-link-to-new-document  (all (only :accounting-write))
            :balance-write                    (all (only :accounting-gc-new-branch))
            :accounting-gc-new-branch         (all (only :accounting-gc-link-to-new-branch))
            :accounting-gc-link-to-new-branch (all (generate-incoming single-threaded
                                                                      [:process {:amount 1}]
                                                                      [:process {:amount -1}])
                                                   (prevents :accounting-gc-link-to-new-branch))}))

(def single-threaded+inmemory-balance+eventually-consistent-accounting-model
  (partial make-model
           {ALWAYS            (all (generate-incoming single-threaded
                                                      [:process {:amount 1}]
                                                      [:process {:amount -1}]))
            :restart          (all (only))
            :process          (& (choose (for-every-past :restart)
                                         (triggers :accounting-read))
                                 (all (prevents :process)))
            :accounting-read  (& (choose (for-every-past :restart)
                                         (triggers :accounting-write))
                                 (all (prevents :accounting-read)))
            :accounting-write (& (choose (for-every-past :restart)
                                         (triggers :balance-write))
                                 (all (prevents :accounting-write)))
            :balance-write    (& (choose (for-every-past :restart)
                                         (only))
                                 (all (prevents :balance-write)))}))