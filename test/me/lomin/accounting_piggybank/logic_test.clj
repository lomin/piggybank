(ns me.lomin.accounting-piggybank.logic-test
  (:require [clojure.test :refer :all]
            [me.lomin.accounting-piggybank.model.core :as timeline]
            [me.lomin.accounting-piggybank.model.logic :refer [for-all there-exists]]))

(deftest ^:unit logic-test
  (let [empty-timeline []
        timeline-0 [[:process {:process-id 1}] [:balance-write {:process-id 1}]]
        timeline-1 [[:process {:process-id 1}] [:balance-write {:process-id 1}] [:process {:process-id 2}]]]

    (is (= true
           (for-all [user-event (timeline/find-events empty-timeline :process)]
                    (there-exists [state-write-event (timeline/find-events empty-timeline :balance-write)]
                                  (= (timeline/get-process-id user-event)
                                     (timeline/get-process-id state-write-event))))))

    (is (= true
           (for-all [user-event (timeline/find-events timeline-0 :process)]
                    (there-exists [state-write-event (timeline/find-events timeline-0 :balance-write)]
                                  (= (timeline/get-process-id user-event)
                                     (timeline/get-process-id state-write-event))))))

    (is (= false
           (for-all [user-event (timeline/find-events timeline-1 :process)]
                    (there-exists [state-write-event (timeline/find-events timeline-1 :balance-write)]
                                  (= (timeline/get-process-id user-event)
                                     (timeline/get-process-id state-write-event))))))))