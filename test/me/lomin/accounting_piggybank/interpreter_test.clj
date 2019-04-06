(ns me.lomin.accounting-piggybank.interpreter-test
  (:require [clojure.test :refer :all]
            [me.lomin.accounting-piggybank.interpreter.core :as intp]
            [me.lomin.accounting-piggybank.interpreter.spec :as spec]))

(def test-state spec/example-context)

(deftest ^:unit interpret-event-test
  (testing "unknown events are ignored"
    (is (= test-state
           (intp/interpret-event test-state [:unknown]))))

  (testing "last document is saved under uuid "
    (is (= {:7 {:last-document {:data [[#{:some-id-1} -1]]
                                :next [:branch-1 :document-1]
                                :self [:branch-1 :document-1]}}}
           (-> test-state
               (intp/interpret-event [:db-read {:id 7}])
               (select-keys [:7])))))

  (testing "update is appended to last document"
    (is (= {:7          {:last-document {:data [[#{:some-id-1} -1] [#{:7} 5]]
                                         :next [:branch-1 :document-1]
                                         :self [:branch-1 :document-1]}}
            :accounting {:branch-0 {:document-0 {:data [[#{:some-id-0} 2]]
                                                 :next         [:branch-1 :document-1]
                                                 :self         [:branch-0 :document-0]}}
                         :branch-1         {:document-0 {:data [[#{:some-id-0} 2]]
                                                         :next [:branch-1 :document-1]
                                                         :self [:branch-0 :document-0]}
                                            :document-1 {:data [[#{:some-id-1} -1] [#{:7} 5]]
                                                         :next [:branch-1 :document-1]
                                                         :self [:branch-1 :document-1]}}
                         :meta             {:meta-document {:branch-0 [:branch-0 :document-0]
                                                            :branch-1 [:branch-1 :document-0]
                                                            :first [:branch-0 :document-0]}}}
            :state      {:events #{:some-id-0 :some-id-1}, :val 1}}
           (-> test-state
               (intp/interpret-event [:db-read {:id 7}])
               (intp/interpret-event [:db-write {:id 7 :val 5}])))))

  (testing "state is negative, hence no updates to db"
    (is (= test-state
           (-> test-state
               (intp/interpret-event [:user {:id 7 :val -10}])
               (intp/interpret-event [:db-read {:id 7}])
               (intp/interpret-event [:db-write {:id 7 :val -10}])
               (dissoc :7))))))

(deftest ^:unit interpret-timeline-test
  (let [test-timeline [[:stuttering]
                       [:user {:id 6, :val 1}]
                       [:user {:id 59, :val -1}]
                       [:db-read {:id 59, :val -1}]
                       [:user {:id 835, :val 0}]
                       [:db-read {:id 835, :val 0}]
                       [:db-write {:id 835, :val 0}]]]
    (is (= {:59          {:last-document {:data [[#{:some-id-1} -1]]
                                          :next [:branch-1 :document-1]
                                          :self [:branch-1 :document-1]}}
            :835         {:last-document {:data [[#{:some-id-1} -1] [#{:835} 0]]
                                          :next [:branch-1 :document-1]
                                          :self [:branch-1 :document-1]}}
            :check-count 7
            :accounting  {:branch-0 {:document-0 {:data [[#{:some-id-0} 2]]
                                                  :next [:branch-1 :document-1]
                                                  :self [:branch-0 :document-0]}}
                          :branch-1 {:document-0 {:data [[#{:some-id-0} 2]]
                                                  :next [:branch-1 :document-1]
                                                  :self [:branch-0 :document-0]}
                                     :document-1 {:data [[#{:some-id-1} -1] [#{:835} 0]]
                                                  :next [:branch-1 :document-1]
                                                  :self [:branch-1 :document-1]}}
                          :meta     {:meta-document {:branch-0 [:branch-0 :document-0]
                                                     :branch-1 [:branch-1 :document-0]
                                                     :first    [:branch-0 :document-0]}}}
            :state       {:events #{:some-id-0 :some-id-1}, :val 1}}
           (-> (intp/interpret-timeline test-state
                                        test-timeline)
               (dissoc :previous-state))))))

(deftest ^:unit interpret-timeline-with-property-violations-test
  (let [test-timeline [[:stuttering]
                       [:user {:id 0, :val -1}]
                       [:user {:id 1, :val -1}]
                       [:db-read {:id 0, :val -1}]
                       [:db-write {:id 0, :val -1}]
                       [:db-read {:id 1, :val -1}]
                       [:db-write {:id 1, :val -1}]
                       [:state-write {:id 0, :val -1}]
                       [:state-write {:id 1, :val -1}]]]
    (is (= {:property-violated {:name     :db-state-must-always-be>=0
                                :timeline [[:stuttering]
                                           [:user {:id 0, :val -1}]
                                           [:user {:id 1, :val -1}]
                                           [:db-read {:id 0, :val -1}]
                                           [:db-write {:id 0, :val -1}]
                                           [:db-read {:id 1, :val -1}]
                                           [:db-write {:id 1, :val -1}]
                                           [:state-write {:id 0, :val -1}]
                                           [:state-write {:id 1, :val -1}]]}}
           (-> test-state
               (intp/interpret-timeline test-timeline)
               (select-keys [:property-violated]))))))

(deftest ^:unit interpret-pagination-test
  (is (= {:0           {:last-document {:data [[#{:some-id-1} -1]]
                                        :next [:branch-1 :document-1]
                                        :self [:branch-1 :document-1]}}
          :check-count 3
          :accounting  {:branch-0 {:document-0 {:data [[#{:some-id-0} 2]]
                                                :next          [:branch-1 :document-1]
                                                :self          [:branch-0 :document-0]}}
                        :branch-1          {:0 {:data [], :next [:branch-1 :0], :self [:branch-1 :0]}
                                            :document-0 {:data [[#{:some-id-0} 2]]
                                                         :next [:branch-1 :document-1]
                                                         :self [:branch-0 :document-0]}
                                            :document-1 {:data [[#{:some-id-1} -1]]
                                                         :next [:branch-1 :document-1]
                                                         :self [:branch-1 :document-1]}}
                        :meta              {:meta-document {:branch-0 [:branch-0 :document-0]
                                                            :branch-1 [:branch-1 :document-0]
                                                            :first [:branch-0 :document-0]}}}
          :state       {:events #{:some-id-0 :some-id-1}, :val 1}}
         (-> (intp/interpret-timeline test-state [[:user {:id 0, :val 1}]
                                                  [:db-read {:id 0, :val 1}]
                                                  [:db-add-new-document {:id 0, :val 1}]])
             (dissoc :previous-state))))

  (is (= {:0           {:last-document {:data [[#{:some-id-1} -1]]
                                        :next [:branch-1 :document-1]
                                        :self [:branch-1 :document-1]}}
          :check-count 4
          :accounting  {:branch-0 {:document-0 {:data [[#{:some-id-0} 2]]
                                                :next          [:branch-1 :document-1]
                                                :self          [:branch-0 :document-0]}}
                        :branch-1          {:0 {:data [], :next [:branch-1 :0], :self [:branch-1 :0]}
                                            :document-0 {:data [[#{:some-id-0} 2]]
                                                         :next [:branch-1 :document-1]
                                                         :self [:branch-0 :document-0]}
                                            :document-1 {:data [[#{:some-id-1} -1]]
                                                         :next [:branch-1 :0]
                                                         :self [:branch-1 :document-1]}}
                        :meta              {:meta-document {:branch-0 [:branch-0 :document-0]
                                                            :branch-1 [:branch-1 :document-0]
                                                            :first [:branch-0 :document-0]}}}
          :state       {:events #{:some-id-0 :some-id-1}, :val 1}}
         (-> (intp/interpret-timeline test-state [[:user {:id 0, :val 1}]
                                                  [:db-read {:id 0, :val 1}]
                                                  [:db-add-new-document {:id 0, :val 1}]
                                                  [:db-link-to-new-document {:id 0, :val 1}]])
             (dissoc :previous-state)))))