(ns me.lomin.piggybank.logic-test
  (:require [clojure.test :refer :all]
            [me.lomin.piggybank.logic :refer [for-all there-exists]]
            [me.lomin.piggybank.model :as timeline]))

(deftest ^:unit logic-test
  (let [empty-timeline []
        timeline-0 [[:process {:process-id 1}] [:terminate/balance-write {:process-id 1}]]
        timeline-1 [[:process {:process-id 1}] [:terminate/balance-write {:process-id 1}] [:process {:process-id 2}]]]

    (is (= true
           (for-all [user-event (timeline/find-events= empty-timeline :process)]
                    (there-exists [state-write-event (timeline/find-events= empty-timeline :terminate/balance-write)]
                                  (= (timeline/get-process-id user-event)
                                     (timeline/get-process-id state-write-event))))))

    (is (= true
           (for-all [user-event (timeline/find-events= timeline-0 :process)]
                    (there-exists [state-write-event (timeline/find-events= timeline-0 :terminate/balance-write)]
                                  (= (timeline/get-process-id user-event)
                                     (timeline/get-process-id state-write-event))))))

    (is (= false
           (for-all [user-event (timeline/find-events= timeline-1 :process)]
                    (there-exists [state-write-event (timeline/find-events= timeline-1 :terminate/balance-write)]
                                  (= (timeline/get-process-id user-event)
                                     (timeline/get-process-id state-write-event))))))))