(ns me.lomin.piggybank.accounting.timeline-test
  (:require [clojure.test :refer :all]
            [me.lomin.piggybank.accounting.model.core :as model]
            [me.lomin.piggybank.model :refer [all
                                              START
                                              always
                                              choose
                                              triggers-for-every-past
                                              generate-incoming
                                              make-model
                                              multi-threaded
                                              only
                                              triggers]]
            [me.lomin.piggybank.timeline :as timeline]
            [orchestra.spec.test :as orchestra]))

;(orchestra/instrument)
(orchestra/unstrument)

(defn successor-timelines [model timeline]
  (timeline/successor-timelines [model timeline]))

(deftest ^:unit multi-threaded-timeline-test
  (is (= 44453
         (count (timeline/all-timelines-of-length 7 model/multi-threaded-simple-model))))
  (is (= #{[[:stuttering]]
           [[:process {:process-id 0, :amount 1}]]
           [[:process {:process-id 1, :amount -1}]]}
         (successor-timelines model/multi-threaded-simple-model
                              [])))

  (is (= #{[[:stuttering] [:stuttering]]
           [[:stuttering] [:process {:process-id 0, :amount 1}]]
           [[:stuttering] [:process {:process-id 1, :amount -1}]]}
         (successor-timelines model/multi-threaded-simple-model
                              [[:stuttering]])))

  (is (= #{[[:stuttering] [:stuttering] [:stuttering]]
           [[:stuttering] [:stuttering] [:process {:process-id 0, :amount 1}]]
           [[:stuttering] [:stuttering] [:process {:process-id 1, :amount -1}]]}
         (successor-timelines model/multi-threaded-simple-model
                              [[:stuttering] [:stuttering]])))

  (is (= #{[[:process {:process-id 0, :amount 1}] [:accounting-read {:process-id 0, :amount 1}]]
           [[:process {:process-id 0, :amount 1}] [:stuttering]]
           [[:process {:process-id 0, :amount 1}] [:process {:process-id 1, :amount 1}]]
           [[:process {:process-id 0, :amount 1}] [:process {:process-id 2, :amount -1}]]}
         (successor-timelines model/multi-threaded-simple-model
                              [[:process {:process-id 0 :amount 1}]])))

  (is (= #{[[:process {:process-id 0, :amount 1}] [:accounting-read {:process-id 0, :amount 1}] [:accounting-write {:process-id 0, :amount 1}]]
           [[:process {:process-id 0, :amount 1}] [:accounting-read {:process-id 0, :amount 1}] [:stuttering]]
           [[:process {:process-id 0, :amount 1}] [:accounting-read {:process-id 0, :amount 1}] [:process {:process-id 1, :amount 1}]]
           [[:process {:process-id 0, :amount 1}] [:accounting-read {:process-id 0, :amount 1}] [:process {:process-id 2, :amount -1}]]}
         (successor-timelines model/multi-threaded-simple-model
                              [[:process {:process-id 0 :amount 1}] [:accounting-read {:process-id 0 :amount 1}]]))))

(deftest ^:unit make-all-timeline-test
  (is (= #{[[:stuttering] [:stuttering]]
           [[:stuttering] [:process {:process-id 0, :amount 1}]]
           [[:stuttering] [:process {:process-id 1, :amount -1}]]
           [[:process {:process-id 0, :amount 1}] [:accounting-read {:process-id 0, :amount 1}]]
           [[:process {:process-id 0, :amount 1}] [:stuttering]]
           [[:process {:process-id 0, :amount 1}] [:process {:process-id 1, :amount 1}]]
           [[:process {:process-id 0, :amount 1}] [:process {:process-id 2, :amount -1}]]}
         (second (timeline/infinite-timelines-seq model/multi-threaded-simple-model
                                                  #{[[:stuttering]] [[:process {:process-id 0 :amount 1}]]}))))

  (is (= #{[[:stuttering] [:stuttering]]
           [[:stuttering] [:process {:process-id 0, :amount 1}]]
           [[:stuttering] [:process {:process-id 1, :amount -1}]]
           [[:process {:process-id 0, :amount 1}] [:accounting-read {:process-id 0, :amount 1}]]
           [[:process {:process-id 0, :amount 1}] [:stuttering]]
           [[:process {:process-id 0, :amount 1}] [:process {:process-id 1, :amount 1}]]
           [[:process {:process-id 0, :amount 1}] [:process {:process-id 2, :amount -1}]]
           [[:process {:process-id 1, :amount -1}] [:accounting-read {:process-id 1, :amount -1}]]
           [[:process {:process-id 1, :amount -1}] [:stuttering]]
           [[:process {:process-id 1, :amount -1}] [:process {:process-id 2, :amount 1}]]
           [[:process {:process-id 1, :amount -1}] [:process {:process-id 3, :amount -1}]]}
         (nth (timeline/infinite-timelines-seq model/multi-threaded-simple-model
                                               timeline/EMPTY-TIMELINES)
              2))))

(deftest ^:unit single-threaded-timeline-test
  (is (= 3367
         (count (timeline/all-timelines-of-length 10 model/single-threaded-simple-model))))

  (is (= #{[[:stuttering]]
           [[:process {:process-id 0, :amount 1}]]
           [[:process {:process-id 1, :amount -1}]]}
         (successor-timelines model/single-threaded-simple-model
                              [])))

  (is (= #{[[:stuttering] [:stuttering]]
           [[:stuttering] [:process {:process-id 0, :amount 1}]]
           [[:stuttering] [:process {:process-id 1, :amount -1}]]}
         (successor-timelines model/single-threaded-simple-model
                              [[:stuttering]])))

  (is (= #{[[:stuttering] [:stuttering] [:stuttering]]
           [[:stuttering] [:stuttering] [:process {:process-id 0, :amount 1}]]
           [[:stuttering] [:stuttering] [:process {:process-id 1, :amount -1}]]}
         (successor-timelines model/single-threaded-simple-model
                              [[:stuttering] [:stuttering]])))

  (is (= #{[[:process {:process-id 0, :amount 1}] [:accounting-read {:process-id 0, :amount 1}]]
           [[:process {:process-id 0, :amount 1}] [:stuttering]]}
         (successor-timelines model/single-threaded-simple-model
                              [[:process {:process-id 0 :amount 1}]])))

  (is (= #{[[:process {:process-id 0, :amount 1}] [:accounting-read {:process-id 0, :amount 1}] [:accounting-write {:process-id 0, :amount 1}]]
           [[:process {:process-id 0, :amount 1}] [:accounting-read {:process-id 0, :amount 1}] [:stuttering]]}
         (successor-timelines model/single-threaded-simple-model
                              [[:process {:process-id 0 :amount 1}] [:accounting-read {:process-id 0 :amount 1}]]))))

(deftest ^:unit pagination-test
  (is (= 3
         (count (timeline/all-timelines-of-length 1 model/single-threaded+pagination-model))))

  (is (= 7
         (count (timeline/all-timelines-of-length 2 model/single-threaded+pagination-model))))

  (is (= 19
         (count (timeline/all-timelines-of-length 3 model/single-threaded+pagination-model)))))

(deftest ^:unit timeline-dependent-of-past-test
  (is (= #{[[:process {:amount 1, :process-id 0}]
            [:balance-write {:amount 1, :process-id 0}]
            [:restart {:past 0}]]
           [[:process {:amount 1, :process-id 0}]
            [:balance-write {:amount 1, :process-id 0}]
            [:restart {:past 1}]]
           [[:process {:amount 1, :process-id 0}]
            [:balance-write {:amount 1, :process-id 0}]
            [:restart {:past 2}]]
           [[:process {:amount 1, :process-id 0}]
            [:balance-write {:amount 1, :process-id 0}]
            [:process {:amount 1, :process-id 1}]]}
         (timeline/all-timelines-of-length 3
                                           (partial make-model
                                                    {START          (all (generate-incoming model/single-threaded
                                                                                            [:process {:amount 1}]))
                                                     :restart       (all (only))
                                                     :process       (choose (triggers-for-every-past :restart)
                                                                            (triggers :balance-write))
                                                     :balance-write (choose (triggers-for-every-past :restart)
                                                                            (only))})))))