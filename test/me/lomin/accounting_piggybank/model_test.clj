(ns me.lomin.accounting-piggybank.model-test
  (:require [clojure.core.reducers :as r]
            [clojure.test :refer :all]
            [me.lomin.accounting-piggybank.interpreter.core :as intp]
            [me.lomin.accounting-piggybank.interpreter.spec :as spec]
            [me.lomin.accounting-piggybank.model.core :refer [&*
                                                              all
                                                              always
                                                              choose
                                                              generate-incoming
                                                              make-model
                                                              multi-threaded
                                                              prevents
                                                              triggers] :as model]
            [me.lomin.accounting-piggybank.progress-bar :as pgb]
            [me.lomin.accounting-piggybank.test-util :refer [=*]]
            [me.lomin.accounting-piggybank.timeline.core :as timeline]))

(def plus (fnil + 0 0))

(defn check-properties
  ([] spec/example-universe)
  ([state] state)
  ([state-0 state-1]
   (let [state-1* (update state-1 :check-count plus (:check-count state-0))]
     (if (:property-violated state-1)
       (reduced state-1*)
       state-1*))))

(defn check
  ([model length]
   (check model length nil))
  ([model length keys]
   (let [timelines (timeline/all-timelines-of-length length model)
         max-check-count (* length (count timelines))
         progress-bar (pgb/make-fuzzy-progress-bar max-check-count)
         result (-> (r/fold check-properties
                            (r/map (partial intp/interpret-timeline
                                            progress-bar
                                            spec/empty-universe)
                                   timelines))
                    (assoc :max-check-count max-check-count))]
     (if keys
       (select-keys result keys)
       result))))

(deftest ^:unit model-unit-test
  (is (= #{[[:stuttering] [:stuttering]]
           [[:stuttering] [:user {:process-id 0, :amount 1}]]
           [[:stuttering] [:user {:process-id 1, :amount -1}]]
           [[:user {:process-id 0, :amount 1}] [:db-read {:process-id 0, :amount 1}]]
           [[:user {:process-id 0, :amount 1}] [:stuttering]]
           [[:user {:process-id 0, :amount 1}] [:user {:process-id 1, :amount 1}]]
           [[:user {:process-id 0, :amount 1}] [:user {:process-id 2, :amount -1}]]
           [[:user {:process-id 1, :amount -1}] [:db-read {:process-id 1, :amount -1}]]
           [[:user {:process-id 1, :amount -1}] [:stuttering]]
           [[:user {:process-id 1, :amount -1}] [:user {:process-id 2, :amount 1}]]
           [[:user {:process-id 1, :amount -1}] [:user {:process-id 3, :amount -1}]]}
         (timeline/all-timelines-of-length 2 model/multi-threaded-simple-model)))
  (is (= #{[[:stuttering] [:stuttering]]
           [[:stuttering] [:user {:process-id 0, :amount 1}]]
           [[:stuttering] [:user {:process-id 1, :amount -1}]]
           [[:user {:process-id 0, :amount 1}] [:db-read {:process-id 0, :amount 1}]]
           [[:user {:process-id 0, :amount 1}] [:stuttering]]
           [[:user {:process-id 1, :amount -1}] [:db-read {:process-id 1, :amount -1}]]
           [[:user {:process-id 1, :amount -1}] [:stuttering]]}
         (timeline/all-timelines-of-length 2 model/single-threaded-simple-model))))

(deftest ^:unit choose-test
  (let [choose-model (partial make-model
                              {::model/always (all (generate-incoming multi-threaded
                                                                      [:user {:amount 1}]
                                                                      [:user {:amount -1}])
                                                   (always [:stuttering]))
                               :user          (choose (&* (triggers :db-read)
                                                          (prevents :user))
                                                      (&* (triggers :state-write)
                                                          (prevents :user)))
                               :db-read       (all (prevents :db-read))
                               :state-write   (all (prevents :state-write))})
        choose-timelines (timeline/all-timelines-of-length 3
                                                           choose-model)]
    (is (= nil
           (seq (filter (fn [[_ a b]]
                          (and (= a :db-read)
                               (= b :state-write)))
                        choose-timelines))))

    (is (= nil
           (seq (filter (fn [[_ a b]]
                          (and (= a :state-write)
                               (= b :db-read)))
                        choose-timelines))))))

(deftest ^:model multi-threaded-model-test
  (is (= {:accounting        {[:cash-up 0]     {[:document 0] {:next      {:cash-up-id 0, :document-id 0}
                                                               :self      {:cash-up-id 0, :document-id 0}
                                                               :transfers [[#{1} 1]]}}
                              [:cash-up :meta] {[:document :meta] {[:cash-up 0]      {:cash-up-id  0
                                                                                      :document-id 0}
                                                                   [:cash-up :start] {:cash-up-id  0
                                                                                      :document-id 0}}}}
          :balance           {:amount 1, :events #{0}}
          :check-count       12536
          :max-check-count   311171
          :property-violated {:name     :there-must-be-no-lost-updates
                              :timeline [[:user {:amount 1, :process-id 0}]
                                         [:user {:amount 1, :process-id 1}]
                                         [:db-read {:amount 1, :process-id 0}]
                                         [:db-read {:amount 1, :process-id 1}]
                                         [:db-write {:amount 1, :process-id 0}]
                                         [:db-write {:amount 1, :process-id 1}]
                                         [:state-write {:amount 1, :process-id 0}]]}}
         (check model/multi-threaded-simple-model 7 [:check-count :property-violated :accounting :max-check-count :balance]))))

(deftest ^:model single-threaded-model-test
  (is (= {:check-count 33670 :max-check-count 33670}
         (check model/single-threaded-simple-model 10 [:check-count :max-check-count :property-violated]))))

(deftest ^:model single-threaded+pagination-model-test
  (is (=* {:accounting        {[:cash-up 0] {[:document 0] {:next      {:cash-up-id 0, :document-id 1}
                                                            :self      {:cash-up-id 0, :document-id 0}
                                                            :transfers [[#{0} 1]]}}}
           :check-count       7
           :property-violated {:name     :all-links-must-point-to-an-existing-document
                               :timeline [[:stuttering]
                                          [:user {:amount 1, :process-id 0}]
                                          [:db-read {:amount 1, :process-id 0}]
                                          [:db-write {:amount 1, :process-id 0}]
                                          [:stuttering]
                                          [:state-write {:amount 1, :process-id 0}]
                                          [:user {:amount 1, :process-id 1}]
                                          [:db-link-to-new-document {:amount 1, :process-id 0}]]}}
          (check model/single-threaded+pagination-model 8 [:check-count :property-violated :accounting]))))

(deftest ^:model single-threaded+safe-pagination-model-test
  (is (= {:check-count 52374}
         (check model/single-threaded+safe-pagination-model 6 [:check-count :property-violated]))))

(deftest ^:model single-threaded+safe-pagination+gc-model-test
  (testing "proves that the garbage collection algorithm is flawed"
    (is (= {:accounting        {[:cash-up 0]     {[:document 0]        {:next      {:cash-up-id 0, :document-id 0}
                                                                        :self      {:cash-up-id 0, :document-id 0}
                                                                        :transfers [[#{0} 1] [#{2} -1]]}
                                                  [:document "0-init"] {:next      {:cash-up-id  0
                                                                                    :document-id "0-init"}
                                                                        :self      {:cash-up-id  0
                                                                                    :document-id "0-init"}
                                                                        :transfers [[#{0} 1]]}}
                                [:cash-up :meta] {[:document :meta] {[:cash-up 0]      {:cash-up-id  0
                                                                                        :document-id "0-init"}
                                                                     [:cash-up :start] {:cash-up-id  0
                                                                                        :document-id "0-init"}}}}
            :check-count       15709
            :property-violated {:name     :there-must-be-no-lost-updates
                                :timeline [[:user {:amount 1, :process-id 0}]
                                           [:db-read {:amount 1, :process-id 0}]
                                           [:db-write {:amount 1, :process-id 0}]
                                           [:state-write {:amount 1, :process-id 0}]
                                           [:db-gc-new-branch {:amount 1, :process-id 0}]
                                           [:user {:amount -1, :process-id 2}]
                                           [:db-read {:amount -1, :process-id 2}]
                                           [:db-gc-link-to-new-branch
                                            {:amount 1, :process-id 0}]
                                           [:db-write {:amount -1, :process-id 2}]
                                           [:state-write {:amount -1, :process-id 2}]]}}
           (check model/model+safe-pagination+gc-strict 10 [:check-count :property-violated :accounting])))))