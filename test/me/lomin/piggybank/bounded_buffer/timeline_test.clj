(ns me.lomin.piggybank.bounded-buffer.timeline-test
  (:require [clojure.test :refer :all]
            [me.lomin.piggybank.bounded-buffer.model :as model]
            [me.lomin.piggybank.timeline :as timeline]))

(deftest ^:unit pagination-test
  (is (= 3
         (count (timeline/all-timelines-of-length 1 model/simple-bounded-buffer-model))))

  (is (= 9
         (count (timeline/all-timelines-of-length 2 model/simple-bounded-buffer-model))))

  (is (= (timeline/all-timelines-of-length 3 model/simple-bounded-buffer-model)
         (timeline/all-timelines-of-length 3 model/simple-bounded-buffer-model)))

  (is (= 27
         (count (timeline/all-timelines-of-length 3 model/simple-bounded-buffer-model)))))
