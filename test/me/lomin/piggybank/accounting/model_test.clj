(ns me.lomin.piggybank.accounting.model-test
  (:require [clojure.test :refer :all]
            [me.lomin.piggybank.accounting.doc :refer [check]]
            [me.lomin.piggybank.accounting.model :as model]
            [me.lomin.piggybank.accounting.timeline-test :as timeline-test]
            [me.lomin.piggybank.asserts :refer [=*]]
            [me.lomin.piggybank.model :refer [all
                                              always
                                              choose
                                              continue
                                              generate-incoming
                                              make-model
                                              multi-threaded
                                              then]]
            [me.lomin.piggybank.timeline :as timeline]))

(deftest ^:unit model-unit-test
  (is (= #{[[:process {:amount -1, :process-id 0}]
            [:accounting-read {:amount -1, :process-id 0}]]
           [[:process {:amount -1, :process-id 0}]
            [:process {:amount -1, :process-id 1}]]
           [[:process {:amount -1, :process-id 0}] [:process {:amount 1, :process-id 1}]]
           [[:process {:amount 1, :process-id 0}]
            [:accounting-read {:amount 1, :process-id 0}]]
           [[:process {:amount 1, :process-id 0}] [:process {:amount -1, :process-id 1}]]
           [[:process {:amount 1, :process-id 0}] [:process {:amount 1, :process-id 1}]]}
         (timeline/all-timelines-of-length 2 timeline-test/simple-model)))

  (is (= #{[[:process {:amount -1, :process-id 0}]
            [:balance-read {:amount -1, :process-id 0}]]
           [[:process {:amount 1, :process-id 0}]
            [:balance-read {:amount 1, :process-id 0}]]}
         (timeline/all-timelines-of-length 2 model/single-threaded-simple-model))))

(deftest ^:unit choose-test
  (let [choose-model (partial make-model
                              {::model/always   (all (generate-incoming multi-threaded
                                                                        [:process {:amount 1}]
                                                                        [:process {:amount -1}])
                                                     (always [:stuttering]))
                               :process         (choose (then :accounting-read)
                                                        (then :balance-write))
                               :accounting-read (continue)
                               :balance-write   (continue)})
        choose-timelines (timeline/all-timelines-of-length 3
                                                           choose-model)]
    (is (= nil
           (seq (filter (fn [[_ a b]]
                          (and (= a :accounting-read)
                               (= b :balance-write)))
                        choose-timelines))))

    (is (= nil
           (seq (filter (fn [[_ a b]]
                          (and (= a :balance-write)
                               (= b :accounting-read)))
                        choose-timelines))))))

(deftest ^:slow-model multi-threaded-model-test
  (is (=* {:property-violated {:name :there-must-be-no-lost-updates}}
          (check model/multi-threaded-simple-model 9 [:check-count :property-violated :accounting :max-check-count :balance]))))

(deftest ^:slow-model single-threaded-model-test
  (is (=* {:max-check-count 3968}
          (check model/single-threaded-simple-model 31 [:check-count :max-check-count :property-violated]))))

(deftest ^:slow-model single-threaded+pagination-model-test
  (is (=* {:property-violated {:name :all-links-must-point-to-an-existing-document}}
          (check model/single-threaded+pagination-model 10 [:check-count :property-violated :accounting]))))

(deftest ^:slow-model single-threaded+safe-pagination-model-test
  (is (=* {:max-check-count 2620392}
          (check model/single-threaded+safe-pagination-model 8 [:max-check-count :check-count :property-violated]))))

(deftest ^:slow-model single-threaded+safe-pagination+gc-model-test
  (testing "proves that the garbage collection algorithm is flawed"
    (is (=* {:property-violated {:name :there-must-be-no-lost-updates}}
            (check model/model+safe-pagination+gc-strict 12 [:check-count :property-violated :accounting])))))

(deftest ^:model single-threaded-inmemory-db-model-test
  (comment [[:process {:amount 1, :process-id 0}]
            [:balance-read {:amount 1, :process-id 0}]
            [:accounting-read {:amount 1, :process-id 0}]
            [:accounting-write {:amount 1, :process-id 0}]
            [:accounting-read-last-write {:amount 1, :process-id 0}]
            [:balance-write {:amount 1, :process-id 0}]
            [:process {:amount -1, :process-id 1}]
            [:balance-read {:amount -1, :process-id 1}]
            [:accounting-read {:amount -1, :process-id 1}]
            [:accounting-write {:amount -1, :process-id 1}]
            [:accounting-read-last-write {:amount -1, :process-id 1}]
            [:restart {:go-steps-back-in-timeline 1}]
            [:process {:amount -1, :process-id 2}]
            [:balance-read {:amount -1, :process-id 2}]
            [:accounting-read {:amount -1, :process-id 2}]
            [:accounting-write {:amount -1, :process-id 2}]])
  (comment filter #(and (= [:process {:amount 1, :process-id 0}] (first %))
                        ; (= 1 (count (filter (fn [[ev]] (= ev :restart)) %)))
                        (= [:accounting-write {:amount -1, :process-id 2}] (last %)))
           (timeline/all-timelines-of-length 16 model/single-threaded+inmemory-balance+eventually-consistent-accounting-model))
  (is (= 14
         (count (timeline/all-timelines-of-length 3 model/single-threaded+inmemory-balance+eventually-consistent-accounting-model))))
  (is (=* {:property-violated
           {:name :accounting-balance-must-always-be>=0
            :timeline
            [[:process {:amount 1, :process-id 0}]
             [:balance-read {:amount 1, :process-id 0}]
             [:accounting-read {:amount 1, :process-id 0}]
             [:accounting-write {:amount 1, :process-id 0}]
             [:accounting-read-last-write {:amount 1, :process-id 0}]
             [:balance-write {:amount 1, :process-id 0}]
             [:process {:amount -1, :process-id 1}]
             [:balance-read {:amount -1, :process-id 1}]
             [:accounting-read {:amount -1, :process-id 1}]
             [:accounting-write {:amount -1, :process-id 1}]
             [:restart {:go-steps-back-in-timeline 1, :process-id 1}]
             [:process {:amount -1, :process-id 2}]
             [:balance-read {:amount -1, :process-id 2}]
             [:accounting-read {:amount -1, :process-id 2}]
             [:accounting-write {:amount -1, :process-id 2}]]}
           :max-check-count 110100}
          (check model/single-threaded+inmemory-balance+eventually-consistent-accounting-model
                 9
                 [:check-count :property-violated :max-check-count]
                 [[:process {:amount 1, :process-id 0}]
                  [:balance-read {:amount 1, :process-id 0}]
                  [:accounting-read {:amount 1, :process-id 0}]
                  [:accounting-write {:amount 1, :process-id 0}]
                  [:accounting-read-last-write {:amount 1, :process-id 0}]
                  [:balance-write {:amount 1, :process-id 0}]]))))

{:check-count     34378
 :max-check-count 36900
 :property-violated
 {:name     :accounting-balance-must-always-be>=0
  :timeline [[:process {:amount 1, :process-id 0}]
             [:balance-read {:amount 1, :process-id 0}]
             [:accounting-read {:amount 1, :process-id 0}]
             [:accounting-write {:amount 1, :process-id 0}]
             [:accounting-read-last-write
              {:amount 1, :process-id 0}]
             [:balance-write {:amount 1, :process-id 0}]
             [:process {:amount -1, :process-id 0}]
             [:balance-read {:amount -1, :process-id 0}]
             [:accounting-read {:amount -1, :process-id 0}]
             [:accounting-write {:amount -1, :process-id 0}]
             [:restart
              {:go-steps-back-in-timeline 1, :process-id 0}]
             [:process {:amount -1, :process-id 1}]
             [:balance-read {:amount -1, :process-id 1}]
             [:accounting-read {:amount -1, :process-id 1}]
             [:accounting-write {:amount -1, :process-id 1}]]}}