(ns me.lomin.accounting-piggybank.interpreter.spec
  (:require [clojure.spec.alpha :as s]
            [me.lomin.accounting-piggybank.accounting.spec :as db]
            [me.lomin.accounting-piggybank.spec :as spec]))

(s/def ::context (s/keys :req-un [::db/accounting ::state] :opt-un [::previous-state]))
(s/def ::previous-state ::context)
(s/def ::state (s/keys :req-un [::spec/val ::spec/events]))

(comment {:accounting {[:cash-up 0] {[:document 0] {:next {:cash-up 1 :document 1}
                                                    :self {:cash-up 0 :document 0}
                                                    :data [#_...]}}
                       [:cash-up 1] {[:document 0] {:next {:cash-up 1 :document 1}
                                                    :self {:cash-up 1 :document 0}
                                                    :data [#_...]}
                                     [:document 1] {:next {:cash-up 1 :document 1}
                                                    :self {:cash-up 1 :document 1}
                                                    :data [#_...]}}
                       :meta        {:meta-document {:start       {:cash-up 0 :document 0}
                                                     [:cash-up 0] {:cash-up 0 :document 0}
                                                     [:cash-up 1] {:cash-up 1 :document 0}}}}
          :piggybank  {:amount 1
                       :events #{:some-id-0 :some-id-1}}})

(def example-context
  {:accounting {:branch-0 {:document-0 {:next [:branch-1 :document-1]
                                        :self      [:branch-0 :document-0]
                                        :data      [[#{:some-id-0} 2]]}}
                :branch-1      {:document-0 {:next [:branch-1 :document-1]
                                             :self [:branch-0 :document-0]
                                             :data [[#{:some-id-0} 2]]}
                                :document-1 {:next [:branch-1 :document-1]
                                             :self [:branch-1 :document-1]
                                             :data [[#{:some-id-1} -1]]}}
                :meta          {:meta-document {:first    [:branch-0 :document-0]
                                                :branch-0 [:branch-0 :document-0]
                                                :branch-1 [:branch-1 :document-0]}}}
   :state      {:val    1
                :events #{:some-id-0 :some-id-1}}})

(def empty-universe
  {:accounting {:branch-0 {:document-0 {:next [:branch-0 :document-0]
                                        :self      [:branch-0 :document-0]
                                        :data      []}}
                :meta          {:meta-document {:first    [:branch-0 :document-0]
                                                :branch-0 [:branch-0 :document-0]}}}
   :state      {:val    0
                :events #{}}})