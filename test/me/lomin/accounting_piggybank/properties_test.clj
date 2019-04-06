(ns me.lomin.accounting-piggybank.properties-test
  (:require [clojure.test :refer :all]
            [me.lomin.accounting-piggybank.interpreter.properties :as props]
            [me.lomin.accounting-piggybank.interpreter.spec :as spec]))

(deftest ^:unit example-context-does-not-violate-any-property
  (is (= nil
         (props/any-property-violation spec/example-context))))

(deftest ^:unit property-db-state>=0-test
  (is (= true (props/db-state>=0? spec/example-context)))
  (is (= false
         (props/db-state>=0? {:accounting {:branch-0 {:document-0 {:data [[#{:some-id-0} 2]]
                                                                   :next         [:branch-1 :document-1]
                                                                   :self         [:branch-0 :document-0]}}
                                           :branch-1         {:document-1 {:data [[#{:some-id-1} -1] [#{:some-id-2} -2]]
                                                                           :next [:branch-1 :document-1]
                                                                           :self [:branch-1 :document-1]}}
                                           :meta             {:meta-document {:first [:branch-0 :document-0]}}}}))))

(deftest ^:unit property-all-documents-linked-test
  (is (= true (props/all-links-exist? spec/example-context)))
  (is (= false
         (props/all-links-exist? {:accounting {:branch-0 {:document-0 {:next [:branch-1 :document-1]
                                                                       :self         [:branch-0 :document-0]
                                                                       :data         [[#{:some-id-0} 2]]}}
                                               :branch-1         {:document-1 {:next [:branch-1 :not-there]
                                                                               :self [:branch-1 :document-1]
                                                                               :data [[#{:some-id-1} -1]]}}
                                               :meta             {:meta-document {:first [:branch-0 :document-0]}}}}))))

(deftest ^:unit property-no-lost-updates-test
  (is (= false (props/lost-updates? spec/example-context)))
  (is (= true
         (props/lost-updates? {:accounting {:branch-0 {:unlinked {:next [:branch-1 :document-1]
                                                                  :self    [:branch-0 :document-0]
                                                                  :data    [[#{:unlinked} 0]]}
                                                       :document-0    {:next [:branch-1 :document-1]
                                                                       :self [:branch-0 :document-0]
                                                                       :data [[#{:some-id-0} 2]]}}
                                            :branch-1      {:document-1 {:next [:branch-1 :document-1]
                                                                         :self [:branch-1 :document-1]
                                                                         :data [[#{:some-id-1} -1]]}}
                                            :meta          {:meta-document {:first [:branch-0 :document-0]}}}
                               :state      {:val    1
                                            :events #{:some-id-0 :some-id-1 :unlinked}}}))))

(deftest ^:unit document-database-is-only-on-consistent-on-document-level-test
  (is (= false (props/more-than-one-document-change-per-timeslot? spec/example-context)))
  (is (= false
         (props/more-than-one-document-change-per-timeslot? (assoc spec/example-context
                                                                   :previous-state spec/example-context))))
  (is (= false
         (props/more-than-one-document-change-per-timeslot? (-> spec/example-context
                                                                (assoc :previous-state spec/example-context)
                                                                (assoc-in [:accounting :branch-0 :document-0 :test] "test")))))
  (is (= true
         (props/more-than-one-document-change-per-timeslot? (-> spec/example-context
                                                                (assoc :previous-state spec/example-context)
                                                                (assoc-in [:accounting :branch-0 :document-0 :test] "test")
                                                                (assoc-in [:accounting :branch-1 :document-1 :test] "test")))))

  (is (= true
         (props/more-than-one-document-change-per-timeslot? (-> spec/example-context
                                                                (assoc :previous-state spec/example-context)
                                                                (assoc-in [:accounting :branch-0 :document-0 :test] "test")
                                                                (assoc-in [:accounting :meta :meta-document :test] "test"))))))
