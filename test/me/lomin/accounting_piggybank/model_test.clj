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
            [me.lomin.accounting-piggybank.timeline.core :as timeline]))

(def plus (fnil + 0 0))

(defn check-properties
  ([] spec/example-context)
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
           [[:stuttering] [:user {:id 0, :amount 1}]]
           [[:stuttering] [:user {:id 1, :amount -1}]]
           [[:user {:id 0, :amount 1}] [:db-read {:id 0, :amount 1}]]
           [[:user {:id 0, :amount 1}] [:stuttering]]
           [[:user {:id 0, :amount 1}] [:user {:id 1, :amount 1}]]
           [[:user {:id 0, :amount 1}] [:user {:id 2, :amount -1}]]
           [[:user {:id 1, :amount -1}] [:db-read {:id 1, :amount -1}]]
           [[:user {:id 1, :amount -1}] [:stuttering]]
           [[:user {:id 1, :amount -1}] [:user {:id 2, :amount 1}]]
           [[:user {:id 1, :amount -1}] [:user {:id 3, :amount -1}]]}
         (timeline/all-timelines-of-length 2 model/multi-threaded-simple-model)))
  (is (= #{[[:stuttering] [:stuttering]]
           [[:stuttering] [:user {:id 0, :amount 1}]]
           [[:stuttering] [:user {:id 1, :amount -1}]]
           [[:user {:id 0, :amount 1}] [:db-read {:id 0, :amount 1}]]
           [[:user {:id 0, :amount 1}] [:stuttering]]
           [[:user {:id 1, :amount -1}] [:db-read {:id 1, :amount -1}]]
           [[:user {:id 1, :amount -1}] [:stuttering]]}
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
  (is (= {:check-count       951
          :accounting        {:branch-0 {:document-0 {:transfers [[#{:some-id-0} 2]]
                                                      :next      [:branch-1 :document-1]
                                                      :self      [:branch-0 :document-0]}}
                              :branch-1 {:document-0 {:transfers [[#{:some-id-0} 2]]
                                                      :next      [:branch-1 :document-1]
                                                      :self      [:branch-0 :document-0]}
                                         :document-1 {:transfers [[#{:some-id-1} -1] [#{:2} -1] [#{:4} -1]]
                                                      :next      [:branch-1 :document-1]
                                                      :self      [:branch-1 :document-1]}}
                              :meta     {:meta-document {:branch-0 [:branch-0 :document-0]
                                                         :branch-1 [:branch-1 :document-0]
                                                         :first    [:branch-0 :document-0]}}}
          :property-violated {:name     :db-state-must-always-be>=0
                              :timeline [[:user {:id 0, :amount 1}]
                                         [:user {:id 2, :amount -1}]
                                         [:db-read {:id 2, :amount -1}]
                                         [:db-write {:id 2, :amount -1}]
                                         [:user {:id 4, :amount -1}]
                                         [:db-read {:id 4, :amount -1}]
                                         [:db-write {:id 4, :amount -1}]]}}
         (check model/multi-threaded-simple-model 7 [:check-count :property-violated :accounting :max-check-count :balance]))))

(deftest ^:model single-threaded-model-test
  (is (= {:check-count 33670 :max-check-count 33670}
         (check model/single-threaded-simple-model 10 [:check-count :max-check-count :property-violated]))))

(deftest ^:model single-threaded+pagination-model-test
  (is (= {:check-count       15
          :accounting        {:branch-0 {:document-0 {:transfers [[#{:some-id-0} 2]]
                                                      :next      [:branch-1 :document-1]
                                                      :self      [:branch-0 :document-0]}}
                              :branch-1 {:1          {:transfers [], :next [:branch-1 :1], :self [:branch-1 :1]}
                                         :document-0 {:transfers [[#{:some-id-0} 2]]
                                                      :next      [:branch-1 :document-1]
                                                      :self      [:branch-0 :document-0]}
                                         :document-1 {:transfers [[#{:some-id-1} -1] [#{:1} -1]]
                                                      :next      [:branch-1 :2]
                                                      :self      [:branch-1 :document-1]}}
                              :meta     {:meta-document {:branch-0 [:branch-0 :document-0]
                                                         :branch-1 [:branch-1 :document-0]
                                                         :first    [:branch-0 :document-0]}}}
          :property-violated {:name     :all-links-must-point-to-an-existing-document
                              :timeline [[:user {:id 1, :amount -1}]
                                         [:db-read {:id 1, :amount -1}]
                                         [:db-write {:id 1, :amount -1}]
                                         [:db-add-new-document {:id 1, :amount -1}]
                                         [:state-write {:id 1, :amount -1}]
                                         [:user {:id 2, :amount 1}]
                                         [:db-read {:id 2, :amount 1}]
                                         [:db-link-to-new-document {:id 2, :amount 1}]]}}
         (check model/single-threaded+pagination-model 8 [:check-count :property-violated :accounting]))))

(deftest ^:model single-threaded+safe-pagination-model-test
  (is (= {:check-count 64030}
         (check model/single-threaded+safe-pagination-model 5 [:check-count :property-violated]))))

(deftest ^:model single-threaded+safe-pagination+gc-model-test
  (is (= {:check-count 219}
         (check model/model+safe-pagination+gc 10 [:check-count :property-violated :accounting])))
  (is (= {:check-count 549}
         (check model/model+safe-pagination+gc-strict 10 [:check-count :property-violated :accounting]))))