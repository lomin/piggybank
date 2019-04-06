(ns me.lomin.accounting-piggybank.interpreter.spec
  (:require [clojure.spec.alpha :as s]
            [me.lomin.accounting-piggybank.accounting.spec :as db]
            [me.lomin.accounting-piggybank.spec :as spec]))

(s/def ::universe (s/keys :req-un [::db/accounting ::balance] :opt-un [::previous-state]))
(s/def ::previous-state ::universe)
(s/def ::balance (s/keys :req-un [::spec/amount ::spec/events]))

(def example-universe
  {:accounting {[:cash-up 0]     {[:document 0] {:next      {:cash-up-id 1 :document-id 1}
                                                 :self      {:cash-up-id 0 :document-id 0}
                                                 :transfers [[#{:some-id-0} 2]]}}
                [:cash-up 1]     {[:document 0] {:next      {:cash-up-id 1 :document-id 1}
                                                 :self      {:cash-up-id 1 :document-id 0}
                                                 :transfers [[#{:some-id-0} 2]]}
                                  [:document 1] {:next      {:cash-up-id 1 :document-id 1}
                                                 :self      {:cash-up-id 1 :document-id 1}
                                                 :transfers [[#{:some-id-1} -1]]}}
                [:cash-up :meta] {[:document :meta] {[:cash-up :start] {:cash-up-id 0 :document-id 0}
                                                     [:cash-up 0]      {:cash-up-id 0 :document-id 0}
                                                     [:cash-up 1]      {:cash-up-id 1 :document-id 0}}}}
   :balance    {:amount 1
                :events #{:some-id-0 :some-id-1}}})

(def empty-universe
  {:accounting {[:cash-up 0]     {[:document 0] {:next      {:cash-up-id 0 :document-id 0}
                                                 :self      {:cash-up-id 0 :document-id 0}
                                                 :transfers []}}
                [:cash-up :meta] {[:document :meta] {[:cash-up :start] {:cash-up-id 0 :document-id 0}
                                                     [:cash-up 0]      {:cash-up-id 0 :document-id 0}}}}
   :balance    {:amount 0
                :events #{}}})