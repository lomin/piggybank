(ns me.lomin.accounting-piggybank.interpreter.spec
  (:require [clojure.spec.alpha :as s]
            [me.lomin.accounting-piggybank.accounting.spec :as db]
            [me.lomin.accounting-piggybank.spec :as spec]))

(s/def ::context (s/keys :req-un [::db/accounting ::balance] :opt-un [::previous-state]))
(s/def ::previous-state ::context)
(s/def ::balance (s/keys :req-un [::spec/amount ::spec/events]))

(comment {:accounting {[:cash-up 0] {[:document 0] {:next      {:cash-up 1 :document 1}
                                                    :self      {:cash-up 0 :document 0}
                                                    :transfers [#_...]}}
                       [:cash-up 1] {[:document 0] {:next      {:cash-up 1 :document 1}
                                                    :self      {:cash-up 1 :document 0}
                                                    :transfers [#_...]}
                                     [:document 1] {:next      {:cash-up 1 :document 1}
                                                    :self      {:cash-up 1 :document 1}
                                                    :transfers [#_...]}}
                       :meta        {:meta-document {:start       {:cash-up 0 :document 0}
                                                     [:cash-up 0] {:cash-up 0 :document 0}
                                                     [:cash-up 1] {:cash-up 1 :document 0}}}}
          :balance    {:amount 1
                       :events #{:some-id-0 :some-id-1}}})

(def example-context
  {:accounting {:branch-0 {:document-0 {:next      [:branch-1 :document-1]
                                        :self      [:branch-0 :document-0]
                                        :transfers [[#{:some-id-0} 2]]}}
                :branch-1      {:document-0 {:next      [:branch-1 :document-1]
                                             :self      [:branch-0 :document-0]
                                             :transfers [[#{:some-id-0} 2]]}
                                :document-1 {:next      [:branch-1 :document-1]
                                             :self      [:branch-1 :document-1]
                                             :transfers [[#{:some-id-1} -1]]}}
                :meta          {:meta-document {:first    [:branch-0 :document-0]
                                                :branch-0 [:branch-0 :document-0]
                                                :branch-1 [:branch-1 :document-0]}}}
   :balance    {:amount 1
                :events #{:some-id-0 :some-id-1}}})

(def empty-universe
  {:accounting {:branch-0 {:document-0 {:next      [:branch-0 :document-0]
                                        :self      [:branch-0 :document-0]
                                        :transfers []}}
                :meta          {:meta-document {:first    [:branch-0 :document-0]
                                                :branch-0 [:branch-0 :document-0]}}}
   :balance    {:amount 0
                :events #{}}})