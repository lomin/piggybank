(ns me.lomin.piggybank.accounting.properties-test
  (:require [clojure.test :refer :all]
            [me.lomin.piggybank.accounting.interpreter.properties :as props]
            [me.lomin.piggybank.accounting.interpreter.spec :as spec]))

(deftest ^:unit example-context-does-not-violate-any-property
  (is (= nil
         (props/any-property-violation {:universe spec/example-universe}))))

(deftest ^:unit property-db-state>=0-test
  (is (= true (props/db-state>=0? spec/example-universe)))
  (is (= false
         (props/db-state>=0? {:accounting {[:cash-up 0]     {[:document 0] {:transfers [[#{:some-id-0} 2]]
                                                                            :next      {:cash-up-id 1 :document-id 1}
                                                                            :self      {:cash-up-id 0 :document-id 0}}}
                                           [:cash-up 1]     {[:document 1] {:transfers [[#{:some-id-1} -1] [#{:some-id-2} -2]]
                                                                            :next      {:cash-up-id 1 :document-id 1}
                                                                            :self      {:cash-up-id 1 :document-id 1}}}
                                           [:cash-up :meta] {[:document :meta] {[:cash-up :start] {:cash-up-id 0 :document-id 0}}}}}))))

(deftest ^:unit property-all-documents-linked-test
  (is (= true (props/all-links-exist? spec/example-universe)))
  (is (= false
         (props/all-links-exist? {:accounting {[:cash-up 0]     {[:document 0] {:next      {:cash-up-id 1 :document-id 1}
                                                                                :self      {:cash-up-id 0 :document-id 0}
                                                                                :transfers [[#{:some-id-0} 2]]}}
                                               [:cash-up 1]     {[:document 1] {:next      {:cash-up-id 1 :document-id :not-there}
                                                                                :self      {:cash-up-id 1 :document-id 1}
                                                                                :transfers [[#{:some-id-1} -1]]}}
                                               [:cash-up :meta] {[:document :meta] {[:cash-up :start] {:cash-up-id 0 :document-id 0}}}}}))))

(deftest ^:unit property-no-lost-updates-test
  (is (= false (props/lost-updates? spec/example-universe)))
  (is (= true
         (props/lost-updates? {:accounting {[:cash-up 0]     {[:document :unlinked] {:next      {:cash-up-id 1 :document-id 1}
                                                                                     :self      {:cash-up-id 0 :document-id :unlinked}
                                                                                     :transfers [[#{:unlinked} 0]]}
                                                              [:document 0]         {:next      {:cash-up-id 1 :document-id 1}
                                                                                     :self      {:cash-up-id 0 :document-id 0}
                                                                                     :transfers [[#{:some-id-0} 2]]}}
                                            [:cash-up 1]     {[:document 1] {:next      {:cash-up-id 1 :document-id 1}
                                                                             :self      {:cash-up-id 1 :document-id 1}
                                                                             :transfers [[#{:some-id-1} -1]]}}
                                            [:cash-up :meta] {[:document :meta] {[:cash-up :start] {:cash-up-id 0 :document-id 0}}}}
                               :balance    {:amount    1
                                            :processes #{:some-id-0 :some-id-1 :unlinked}}}))))

(deftest ^:unit document-database-is-only-on-consistent-on-document-level-test
  (is (= false (props/more-than-one-document-change-per-timeslot? spec/example-universe)))
  (is (= false
         (props/more-than-one-document-change-per-timeslot? (assoc spec/example-universe
                                                                   :history
                                                                   (list spec/example-universe)))))
  (is (= false
         (props/more-than-one-document-change-per-timeslot? (-> spec/example-universe
                                                                (assoc :history (list spec/example-universe))
                                                                (assoc-in [:accounting [:cash-up 0] [:document 0] :test] "test")))))
  (is (= true
         (props/more-than-one-document-change-per-timeslot? (-> spec/example-universe
                                                                (assoc :history (list spec/example-universe))
                                                                (assoc-in [:accounting [:cash-up 0] [:document 0] :test] "test")
                                                                (assoc-in [:accounting [:cash-up 1] [:document 1] :test] "test")))))

  (is (= true
         (props/more-than-one-document-change-per-timeslot? (-> spec/example-universe
                                                                (assoc :history (list spec/example-universe))
                                                                (assoc-in [:accounting [:cash-up 0] [:document 0] :test] "test")
                                                                (assoc-in [:accounting [:cash-up :meta] [:document :meta] :test] "test"))))))
