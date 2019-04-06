(ns me.lomin.accounting-piggybank.logic-test
  (:require [clojure.test :refer :all]
            [me.lomin.accounting-piggybank.model.core :as timeline]
            [me.lomin.accounting-piggybank.model.logic :refer [for-all there-exists]]))

(deftest ^:unit logic-test
  (let [empty-timeline []
        timeline-0 [[:user {:id 1}] [:state-write {:id 1}]]
        timeline-1 [[:user {:id 1}] [:state-write {:id 1}] [:user {:id 2}]]]

    (is (= true
           (for-all [user-event (timeline/find-events empty-timeline :user)]
                    (there-exists [state-write-event (timeline/find-events empty-timeline :state-write)]
                                  (= (timeline/get-event-id user-event)
                                     (timeline/get-event-id state-write-event))))))

    (is (= true
           (for-all [user-event (timeline/find-events timeline-0 :user)]
                    (there-exists [state-write-event (timeline/find-events timeline-0 :state-write)]
                                  (= (timeline/get-event-id user-event)
                                     (timeline/get-event-id state-write-event))))))

    (is (= false
           (for-all [user-event (timeline/find-events timeline-1 :user)]
                    (there-exists [state-write-event (timeline/find-events timeline-1 :state-write)]
                                  (= (timeline/get-event-id user-event)
                                     (timeline/get-event-id state-write-event))))))))