(ns me.lomin.accounting-piggybank.interpreter-test
  (:require [clojure.test :refer :all]
            [me.lomin.accounting-piggybank.interpreter.core :as intp]
            [me.lomin.accounting-piggybank.interpreter.spec :as spec]
            [me.lomin.accounting-piggybank.test-util :refer [=*]]))

(def test-state spec/example-universe)

(deftest ^:unit interpret-event-test
  (testing "unknown events are ignored"
    (is (= test-state
           (intp/interpret-event test-state [:unknown]))))

  (testing "last document is saved in universe under process-id"
    (is (=* {7 {:last-document {:next      {:cash-up-id 1, :document-id 1}
                                :self      {:cash-up-id 1, :document-id 1}
                                :transfers [[#{:some-id-1} -1]]}}}
            (intp/interpret-event test-state [:db-read {:process-id 7}]))))

  (testing "update is appended to last document"
    (is (=* {:accounting {[:cash-up 1] {[:document 1] {:next      {:cash-up-id 1, :document-id 1}
                                                       :self      {:cash-up-id 1, :document-id 1}
                                                       :transfers [[#{:some-id-1} -1]
                                                                   [#{7} 5]]}}}}
            (-> test-state
                (intp/interpret-event [:db-read {:process-id 7}])
                (intp/interpret-event [:db-write {:process-id 7 :amount 5}])))))

  (testing "state is negative, hence no updates to db"
    (is (=* test-state
            (-> test-state
                (intp/interpret-event [:user {:process-id 7 :amount -10}])
                (intp/interpret-event [:db-read {:process-id 7}])
                (intp/interpret-event [:db-write {:process-id 7 :amount -10}]))))))

(deftest ^:unit interpret-timeline-test
  (let [test-timeline [[:stuttering]
                       [:user {:process-id 6, :amount 1}]
                       [:user {:process-id 59, :amount -1}]
                       [:db-read {:process-id 59, :amount -1}]
                       [:user {:process-id 835, :amount 0}]
                       [:db-read {:process-id 835, :amount 0}]
                       [:db-write {:process-id 835, :amount 0}]]]
    (is (=* {59           {:last-document {:next      {:cash-up-id 1, :document-id 1}
                                           :self      {:cash-up-id 1, :document-id 1}
                                           :transfers [[#{:some-id-1} -1]]}}
             835          {:last-document {:next      {:cash-up-id 1, :document-id 1}
                                           :self      {:cash-up-id 1, :document-id 1}
                                           :transfers [[#{:some-id-1} -1] [#{835} 0]]}}
             :accounting  {[:cash-up 1] {[:document 0] {:next      {:cash-up-id 1, :document-id 1}
                                                        :self      {:cash-up-id 1, :document-id 0}
                                                        :transfers [[#{:some-id-0} 2]]}
                                         [:document 1] {:next      {:cash-up-id 1, :document-id 1}
                                                        :self      {:cash-up-id 1, :document-id 1}
                                                        :transfers [[#{:some-id-1} -1]
                                                                    [#{835} 0]]}}}
             :check-count 7}
            (intp/interpret-timeline test-state
                                     test-timeline)))))

(deftest ^:unit interpret-timeline-with-property-violations-test
  (let [test-timeline [[:stuttering]
                       [:user {:process-id 0, :amount -1}]
                       [:user {:process-id 1, :amount -1}]
                       [:db-read {:process-id 0, :amount -1}]
                       [:db-write {:process-id 0, :amount -1}]
                       [:db-read {:process-id 1, :amount -1}]
                       [:db-write {:process-id 1, :amount -1}]
                       [:state-write {:process-id 0, :amount -1}]
                       [:state-write {:process-id 1, :amount -1}]]]
    (is (= {:property-violated {:name     :db-state-must-always-be>=0
                                :timeline [[:stuttering]
                                           [:user {:process-id 0, :amount -1}]
                                           [:user {:process-id 1, :amount -1}]
                                           [:db-read {:process-id 0, :amount -1}]
                                           [:db-write {:process-id 0, :amount -1}]
                                           [:db-read {:process-id 1, :amount -1}]
                                           [:db-write {:process-id 1, :amount -1}]
                                           [:state-write {:process-id 0, :amount -1}]
                                           [:state-write {:process-id 1, :amount -1}]]}}
           (-> test-state
               (intp/interpret-timeline test-timeline)
               (select-keys [:property-violated]))))))

(deftest ^:unit interpret-pagination-test
  (testing "document 7 gets created but not linked"
    (is (=* {:accounting {[:cash-up 1] {[:document 1] {:next {:cash-up-id 1, :document-id 1}}
                                        [:document 8] {:next      {:cash-up-id 1, :document-id 8}
                                                       :self      {:cash-up-id 1, :document-id 8}
                                                       :transfers []}}}}
            (intp/interpret-timeline test-state [[:user {:process-id 7, :amount 1}]
                                                 [:db-read {:process-id 7, :amount 1}]
                                                 [:db-add-new-document {:process-id 7, :amount 1}]]))))

  (testing "document 1 now links to document 7"
    (is (=* {:accounting {[:cash-up 1] {[:document 1] {:next {:cash-up-id 1, :document-id 8}}
                                        [:document 8] {:next      {:cash-up-id 1, :document-id 8}
                                                       :self      {:cash-up-id 1, :document-id 8}
                                                       :transfers []}}}}
            (intp/interpret-timeline test-state [[:user {:process-id 7, :amount 1}]
                                                 [:db-read {:process-id 7, :amount 1}]
                                                 [:db-add-new-document {:process-id 7, :amount 1}]
                                                 [:db-link-to-new-document {:process-id 7, :amount 1}]])))))