(ns me.lomin.piggybank.accounting.timeline-test
  (:require [clojure.test :refer :all]
            [me.lomin.piggybank.accounting.model :as model]
            [me.lomin.piggybank.model :refer [all
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
                                              then-for-every-past-time-slot]]
            [me.lomin.piggybank.timeline :as timeline]
            [orchestra.spec.test :as orchestra]))

;(orchestra/instrument)
(orchestra/unstrument)

(defn successor-timelines [model timeline]
  (timeline/successor-timelines [model timeline]))

(def simple-model
  (make-model
   {START                   (all (generate-incoming multi-threaded
                                                    [:process {:amount 1}]
                                                    [:process {:amount -1}]))
    :process                 (all (then :accounting-read))
    :accounting-read         (all (then :accounting-write))
    :accounting-write        (all (then :terminate/balance-write))
    :terminate/balance-write (continue)}))

(def simple-single-model
  (make-model
   {START                   (all (generate-incoming single-threaded
                                                    [:process {:amount 1}]
                                                    [:process {:amount -1}]))
    :process                 (all (then :accounting-read))
    :accounting-read         (all (then :accounting-write))
    :accounting-write        (all (then :terminate/balance-write))
    :terminate/balance-write (continue)}))

(deftest ^:unit multi-threaded-timeline-test
  (is (= 13932
         (count (timeline/all-timelines-of-length 7 simple-model))))
  (is (= #{[[:process {:process-id 0, :amount 1}]]
           [[:process {:process-id 0, :amount -1}]]}
         (successor-timelines simple-model
                              [])))

  (is (=    #{[[:process {:amount 1, :process-id 0}]
               [:accounting-read {:amount 1, :process-id 0}]
               [:accounting-write {:amount 1, :process-id 0}]]
              [[:process {:amount 1, :process-id 0}]
               [:accounting-read {:amount 1, :process-id 0}]
               [:process {:amount -1, :process-id 1}]]
              [[:process {:amount 1, :process-id 0}]
               [:accounting-read {:amount 1, :process-id 0}]
               [:process {:amount 1, :process-id 1}]]}
            (successor-timelines simple-model
                                 [[:process {:process-id 0 :amount 1}] [:accounting-read {:process-id 0 :amount 1}]]))))

(deftest ^:unit make-all-timeline-test
  (is (= #{[[:process {:amount -1, :process-id 0}]
            [:accounting-read {:amount -1, :process-id 0}]]
           [[:process {:amount -1, :process-id 0}]
            [:process {:amount -1, :process-id 1}]]
           [[:process {:amount -1, :process-id 0}] [:process {:amount 1, :process-id 1}]]
           [[:process {:amount 1, :process-id 0}]
            [:accounting-read {:amount 1, :process-id 0}]]
           [[:process {:amount 1, :process-id 0}] [:process {:amount -1, :process-id 1}]]
           [[:process {:amount 1, :process-id 0}] [:process {:amount 1, :process-id 1}]]}
         (nth (timeline/infinite-timelines-seq simple-model
                                               timeline/EMPTY-TIMELINES)
              2))))

(deftest ^:unit must-start-with-process-id-0-test
  (is (= nil
         (seq (filter (fn [[[_ {process-id :process-id} :as first-event]]]
                        (not= 0 process-id))
                      (timeline/all-timelines-of-length 3 model/example-model-0))))))

(deftest ^:unit single-threaded-timeline-test
  (is (= 8
         (count (timeline/all-timelines-of-length 10 simple-single-model))))

  (is (= #{[[:process {:process-id 0, :amount 1}]]
           [[:process {:process-id 0, :amount -1}]]}
         (successor-timelines simple-model
                              [])))

  (is (= #{[[:process {:process-id 0, :amount 1}] [:accounting-read {:process-id 0, :amount 1}]]}
         (successor-timelines simple-single-model
                              [[:process {:process-id 0 :amount 1}]])))

  (is (= #{[[:process {:process-id 0, :amount 1}] [:accounting-read {:process-id 0, :amount 1}] [:accounting-write {:process-id 0, :amount 1}]]}
         (successor-timelines simple-single-model
                              [[:process {:process-id 0 :amount 1}] [:accounting-read {:process-id 0 :amount 1}]]))))

(deftest ^:unit pagination-test
  (is (= 3
         (count (timeline/all-timelines-of-length 1 model/single-threaded+pagination-model))))

  (is (= 7
         (count (timeline/all-timelines-of-length 2 model/single-threaded+pagination-model))))

  (is (= 15
         (count (timeline/all-timelines-of-length 3 model/single-threaded+pagination-model)))))

(deftest ^:unit timeline-dependent-of-past-test
  (is (= #{[[:process {:amount 1, :process-id 0}]
            [:terminate/balance-write {:amount 1, :process-id 0}]
            [:process {:amount 1, :process-id 1}]]
           [[:process {:amount 1, :process-id 0}]
            [:terminate/balance-write {:amount 1, :process-id 0}]
            [:terminate/restart {:go-steps-back-in-timeline 0, :process-id 0}]]
           [[:process {:amount 1, :process-id 0}]
            [:terminate/balance-write {:amount 1, :process-id 0}]
            [:terminate/restart {:go-steps-back-in-timeline 1, :process-id 0}]]
           [[:process {:amount 1, :process-id 0}]
            [:terminate/restart {:go-steps-back-in-timeline 0, :process-id 0}]
            [:process {:amount 1, :process-id 1}]]
           [[:process {:amount 1, :process-id 0}]
            [:terminate/restart {:go-steps-back-in-timeline 1, :process-id 0}]
            [:process {:amount 1, :process-id 1}]]}
         (timeline/all-timelines-of-length 3
                                           (partial make-model
                                                    {START                    (all (generate-incoming single-threaded
                                                                                                      [:process {:amount 1}]))
                                                     :terminate/restart       (all (restart))
                                                     :process                 (choose (then-for-every-past-time-slot :terminate/restart)
                                                                                      (then :terminate/balance-write))
                                                     :terminate/balance-write (choose (then-for-every-past-time-slot :terminate/restart)
                                                                                      (restart))})))))