(ns me.lomin.piggybank.doc-test
  (:require [clojure.test :refer :all]
            [me.lomin.piggybank.accounting.doc :as acc-doc]
            [me.lomin.piggybank.accounting.interpreter.core :as intp]
            [me.lomin.piggybank.accounting.interpreter.spec :as spec]
            [me.lomin.piggybank.accounting.model :as model]
            [me.lomin.piggybank.asserts :refer [=*]]
            [me.lomin.piggybank.bounded-buffer.doc :as b-doc]
            [me.lomin.piggybank.bounded-buffer.interpreter.core :as b-intp]
            [me.lomin.piggybank.bounded-buffer.model :as b-model]
            [me.lomin.piggybank.bounded-buffer.spec :as b-spec]
            [me.lomin.piggybank.doc :as doc]
            [ubergraph.core :as ugraph]))

(def example-state
  {0                  {:last-document {:test "0"}}
   1                  {:last-document {:test "1"}}
   :accounting        {[:cash-up 0]     {[:document 0] {:next      {:cash-up-id 0, :document-id 1}
                                                        :self      {:cash-up-id 0, :document-id 0}
                                                        :transfers [[#{0} 1]
                                                                    [#{1} -1]]}}
                       [:cash-up :meta] {[:document :meta] {[:cash-up 0]      {:cash-up-id  0
                                                                               :document-id 0}
                                                            [:cash-up :start] {:cash-up-id  0
                                                                               :document-id 0}}}}
   :timeline          [[:stuttering]
                       [:process {:amount 1, :process-id 0}]
                       [:stuttering]
                       [:stuttering]
                       [:accounting-read {:amount 1, :process-id 0}]
                       [:accounting-write {:amount 1, :process-id 1}]
                       [:accounting-write {:amount 1, :process-id 0}]
                       [:accounting-link-to-new-document
                        {:amount 1, :process-id 0}]
                       [:terminate/balance-write {:amount 1, :process-id 0}]]
   :check-count       38
   :property-violated {:name     :all-links-must-point-to-an-existing-document
                       :timeline [[:stuttering]
                                  [:process {:amount 1, :process-id 0}]
                                  [:stuttering]
                                  [:stuttering]
                                  [:accounting-read {:amount 1, :process-id 0}]
                                  [:accounting-write {:amount 1, :process-id 0}]
                                  [:accounting-link-to-new-document
                                   {:amount 1, :process-id 0}]
                                  [:terminate/balance-write {:amount 1, :process-id 0}]]}})

(deftest ^:unit extract-data-from-state-space
  (is (= [0 1] (acc-doc/get-all-process-ids example-state)))
  (is (= {0 {:last-document {:test "0"}}, 1 {:last-document {:test "1"}}}
         (acc-doc/get-all-local-vars example-state)))
  (is (= {0 {:test "0"}, 1 {:test "1"}}
         (acc-doc/get-all-local-documents example-state))))

(deftest ^:unit make-state-space-test
  (let [state-space (doc/make-state-space {:model       model/single-threaded-simple-model
                                           :length      5
                                           :keys        keys
                                           :interpreter intp/interpret-timeline
                                           :universe    spec/empty-universe
                                           :partitions  5
                                           :make-attrs  acc-doc/make-attrs})
        g0 state-space]
    (is (= "start [shape=record, label=\"{{accounting|{process|db}|{document|\\{\\}}}|{balance|{process|db}|{amount|0}}|{completed transfers|}}\"];"
           (acc-doc/make-dot-str g0 doc/ROOT)))

    (is (= "start ->  -1974429183[label=\"[+1€ pid=0]\"];"
           (let [node (nth (ugraph/edges g0) 1)]
             (acc-doc/make-label-dot-str g0 node))))

    (is (= 2
           (count (doc/find-all-leafs g0))))

    (is (= '("start" 127232727 590401434)
           (doc/find-path g0 :invalid-timeline)))))

(deftest ^:unit make-single-threaded-state-space-test
  (let [state-space (doc/make-state-space {:model       model/single-threaded-simple-model
                                           :length      5
                                           :keys        keys
                                           :interpreter intp/interpret-timeline
                                           :universe    spec/empty-universe
                                           :partitions  3
                                           :make-attrs  acc-doc/make-attrs})
        g0 state-space]

    (is (= "start [shape=record, label=\"{{accounting|{process|db}|{document|\\{\\}}}|{balance|{process|db}|{amount|0}}|{completed transfers|}}\"];"
           (acc-doc/make-dot-str g0 doc/ROOT)))

    (is (= "start ->  -1974429183[label=\"[+1€ pid=0]\"];"
           (let [node (nth (ugraph/edges g0) 1)]
             (acc-doc/make-label-dot-str g0 node))))

    (is (= 2
           (count (doc/find-all-leafs g0))))

    (is (= '("start" 127232727 590401434)
           (doc/find-path g0 :invalid-timeline)))))

(defn make-two-producers-graph [length]
  (with-redefs [me.lomin.piggybank.bounded-buffer.spec/BUFFER-LENGTH 1]
    (doc/make-state-space {:model       b-model/two-producers-model
                           :length      length
                           :keys        keys
                           :interpreter b-intp/interpret-timeline
                           :universe    b-spec/empty-universe
                           :make-attrs  (partial b-doc/make-attrs
                                                 [[:consumer {:id 0}]
                                                  [:producer {:id 1}]
                                                  [:producer {:id 2}]])})))

(deftest ^:unit buffer-doc-test
  (let [g (make-two-producers-graph 2)]
    (is (=* {:buffer   "empty"
             :occupied 0
             :put-at   0
             :take-at  0
             :threads
             {0 [:consumer "sleeping"]
              1 [:producer "awake"]
              2 [:producer "awake"]}}
            (ugraph/attrs g (doc/find-node g #(and (= "empty" (:buffer %)))))))
    (is (=* {:buffer   "empty"
             :occupied 0
             :put-at   0
             :take-at  0
             :threads
             {0 [:consumer "awake"]
              1 [:producer "awake"]
              2 [:producer "awake"]}}
            (ugraph/attrs g doc/ROOT)))))

(deftest ^:unit node-label-test
  (is (= "{{accounting|{process|db}|{document|\\{\\}}}|{balance|{process|db}|{amount|0}}|{completed transfers|}}"
         (acc-doc/node-label [["accounting" ["process" "db"] ["document" {}]]
                              ["balance" ["process" "db"] ["amount" 0]]
                              ["completed transfers" ""]])))
  (is (= "{{accounting|{process|db}|{document|\\{0:1\\}}}|{balance|{process|db}|{amount|0}}|{completed transfers|}}"
         (acc-doc/node-label [["accounting" ["process" "db"] ["document" {0 1}]]
                              ["balance" ["process" "db"] ["amount" 0]]
                              ["completed transfers" ""]]))))

(deftest ^:unit state-to-node-test
  (let [state {:accounting          {:db {} 0 {0 1}}
               :balance             {:db 0, 0 0}
               :completed-transfers []}]
    (is (= [:accounting [:process :db 0] [:document {} {0 1}]]
           (acc-doc/state->node state :accounting [:process :document])))
    (is (= [:balance [:process :db 0] [:amount 0 0]]
           (acc-doc/state->node state :balance [:process :amount])))
    (is (= [:completed-transfers ""]
           (acc-doc/state->node state :completed-transfers)))
    (is (= [:completed-transfers 0]
           (acc-doc/state->node {:completed-transfers [0]} :completed-transfers)))))