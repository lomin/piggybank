(ns me.lomin.accounting-piggybank.timeline-test
  (:require [clojure.test :refer :all]
            [me.lomin.accounting-piggybank.model.core :refer [&
                                                              all
                                                              always
                                                              choose
                                                              for-every-past
                                                              generate-incoming
                                                              multi-threaded
                                                              only
                                                              prevents
                                                              single-threaded
                                                              triggers] :as model]
            [me.lomin.accounting-piggybank.timeline.core :as timeline]
            [orchestra.spec.test :as orchestra]))

;(orchestra/instrument)
(orchestra/unstrument)

(defn successor-timelines [model timeline]
  (timeline/successor-timelines [model timeline]))

(deftest ^:unit multi-threaded-timeline-test
  (is (= 44453
         (count (timeline/all-timelines-of-length 7 model/multi-threaded-simple-model))))
  (is (= #{[[:stuttering]]
           [[:user {:process-id 0, :amount 1}]]
           [[:user {:process-id 1, :amount -1}]]}
         (successor-timelines model/multi-threaded-simple-model
                              [])))

  (is (= #{[[:stuttering] [:stuttering]]
           [[:stuttering] [:user {:process-id 0, :amount 1}]]
           [[:stuttering] [:user {:process-id 1, :amount -1}]]}
         (successor-timelines model/multi-threaded-simple-model
                              [[:stuttering]])))

  (is (= #{[[:stuttering] [:stuttering] [:stuttering]]
           [[:stuttering] [:stuttering] [:user {:process-id 0, :amount 1}]]
           [[:stuttering] [:stuttering] [:user {:process-id 1, :amount -1}]]}
         (successor-timelines model/multi-threaded-simple-model
                              [[:stuttering] [:stuttering]])))

  (is (= #{[[:user {:process-id 0, :amount 1}] [:db-read {:process-id 0, :amount 1}]]
           [[:user {:process-id 0, :amount 1}] [:stuttering]]
           [[:user {:process-id 0, :amount 1}] [:user {:process-id 1, :amount 1}]]
           [[:user {:process-id 0, :amount 1}] [:user {:process-id 2, :amount -1}]]}
         (successor-timelines model/multi-threaded-simple-model
                              [[:user {:process-id 0 :amount 1}]])))

  (is (= #{[[:user {:process-id 0, :amount 1}] [:db-read {:process-id 0, :amount 1}] [:db-write {:process-id 0, :amount 1}]]
           [[:user {:process-id 0, :amount 1}] [:db-read {:process-id 0, :amount 1}] [:stuttering]]
           [[:user {:process-id 0, :amount 1}] [:db-read {:process-id 0, :amount 1}] [:user {:process-id 1, :amount 1}]]
           [[:user {:process-id 0, :amount 1}] [:db-read {:process-id 0, :amount 1}] [:user {:process-id 2, :amount -1}]]}
         (successor-timelines model/multi-threaded-simple-model
                              [[:user {:process-id 0 :amount 1}] [:db-read {:process-id 0 :amount 1}]]))))

(deftest ^:unit make-all-timeline-test
  (is (= #{[[:stuttering] [:stuttering]]
           [[:stuttering] [:user {:process-id 0, :amount 1}]]
           [[:stuttering] [:user {:process-id 1, :amount -1}]]
           [[:user {:process-id 0, :amount 1}] [:db-read {:process-id 0, :amount 1}]]
           [[:user {:process-id 0, :amount 1}] [:stuttering]]
           [[:user {:process-id 0, :amount 1}] [:user {:process-id 1, :amount 1}]]
           [[:user {:process-id 0, :amount 1}] [:user {:process-id 2, :amount -1}]]}
         (second (timeline/infinite-timelines-seq model/multi-threaded-simple-model
                                                  #{[[:stuttering]] [[:user {:process-id 0 :amount 1}]]}))))

  (is (= #{[[:stuttering] [:stuttering]]
           [[:stuttering] [:user {:process-id 0, :amount 1}]]
           [[:stuttering] [:user {:process-id 1, :amount -1}]]
           [[:user {:process-id 0, :amount 1}] [:db-read {:process-id 0, :amount 1}]]
           [[:user {:process-id 0, :amount 1}] [:stuttering]]
           [[:user {:process-id 0, :amount 1}] [:user {:process-id 1, :amount 1}]]
           [[:user {:process-id 0, :amount 1}] [:user {:process-id 2, :amount -1}]]
           [[:user {:process-id 1, :amount -1}] [:db-read {:process-id 1, :amount -1}]]
           [[:user {:process-id 1, :amount -1}] [:stuttering]]
           [[:user {:process-id 1, :amount -1}] [:user {:process-id 2, :amount 1}]]
           [[:user {:process-id 1, :amount -1}] [:user {:process-id 3, :amount -1}]]}
         (nth (timeline/infinite-timelines-seq model/multi-threaded-simple-model
                                               timeline/EMPTY-TIMELINES)
              2))))

(deftest ^:unit single-threaded-timeline-test
  (is (= 3367
         (count (timeline/all-timelines-of-length 10 model/single-threaded-simple-model))))

  (is (= #{[[:stuttering]]
           [[:user {:process-id 0, :amount 1}]]
           [[:user {:process-id 1, :amount -1}]]}
         (successor-timelines model/single-threaded-simple-model
                              [])))

  (is (= #{[[:stuttering] [:stuttering]]
           [[:stuttering] [:user {:process-id 0, :amount 1}]]
           [[:stuttering] [:user {:process-id 1, :amount -1}]]}
         (successor-timelines model/single-threaded-simple-model
                              [[:stuttering]])))

  (is (= #{[[:stuttering] [:stuttering] [:stuttering]]
           [[:stuttering] [:stuttering] [:user {:process-id 0, :amount 1}]]
           [[:stuttering] [:stuttering] [:user {:process-id 1, :amount -1}]]}
         (successor-timelines model/single-threaded-simple-model
                              [[:stuttering] [:stuttering]])))

  (is (= #{[[:user {:process-id 0, :amount 1}] [:db-read {:process-id 0, :amount 1}]]
           [[:user {:process-id 0, :amount 1}] [:stuttering]]}
         (successor-timelines model/single-threaded-simple-model
                              [[:user {:process-id 0 :amount 1}]])))

  (is (= #{[[:user {:process-id 0, :amount 1}] [:db-read {:process-id 0, :amount 1}] [:db-write {:process-id 0, :amount 1}]]
           [[:user {:process-id 0, :amount 1}] [:db-read {:process-id 0, :amount 1}] [:stuttering]]}
         (successor-timelines model/single-threaded-simple-model
                              [[:user {:process-id 0 :amount 1}] [:db-read {:process-id 0 :amount 1}]]))))

(deftest ^:unit pagination-test
  (is (= 3
         (count (timeline/all-timelines-of-length 1 model/single-threaded+pagination-model))))

  (is (= 7
         (count (timeline/all-timelines-of-length 2 model/single-threaded+pagination-model))))

  (is (= 19
         (count (timeline/all-timelines-of-length 3 model/single-threaded+pagination-model)))))

(deftest ^:unit timeline-dependent-of-past-test
  (is (=   #{[[:user {:amount 1, :process-id 0}]
              [:state-write {:amount 1, :process-id 0}]
              [:restart {:past 0}]]
             [[:user {:amount 1, :process-id 0}]
              [:state-write {:amount 1, :process-id 0}]
              [:restart {:past 1}]]
             [[:user {:amount 1, :process-id 0}]
              [:state-write {:amount 1, :process-id 0}]
              [:restart {:past 2}]]
             [[:user {:amount 1, :process-id 0}]
              [:state-write {:amount 1, :process-id 0}]
              [:user {:amount 1, :process-id 1}]]}
           (timeline/all-timelines-of-length 3
                                             (partial model/make-model
                                                      {::model/always (all (generate-incoming single-threaded
                                                                                              [:user {:amount 1}]))
                                                       :restart       (all (only))
                                                       :user          (& (choose (for-every-past :restart)
                                                                                 (triggers :state-write))
                                                                         (all (prevents :user)))
                                                       :state-write   (& (choose (for-every-past :restart)
                                                                                 (only))
                                                                         (all (prevents :state-write)))})))))