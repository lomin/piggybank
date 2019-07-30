(ns me.lomin.piggybank.bounded-buffer.model-test
  (:require [clojure.test :refer :all]
            [me.lomin.piggybank.asserts :refer [=*]]
            [me.lomin.piggybank.bounded-buffer.doc :refer [check]]
            [me.lomin.piggybank.bounded-buffer.interpreter.core :as intp]
            [me.lomin.piggybank.bounded-buffer.model :as model]
            [me.lomin.piggybank.bounded-buffer.spec :as spec]
            [me.lomin.piggybank.model :refer [all
                                              always
                                              choose
                                              generate-incoming
                                              make-model
                                              multi-threaded
                                              START
                                              then]]))

(deftest ^:unit
  make-model-test
  (is (= '(([:consumer {:id 0, :notify 0}]
            [:consumer {:id 0, :notify 1}]
            [:consumer {:id 0, :notify 2}]
            [:producer {:id 1, :notify 0}]
            [:producer {:id 1, :notify 1}]
            [:producer {:id 1, :notify 2}]
            [:producer {:id 2, :notify 0}]
            [:producer {:id 2, :notify 1}]
            [:producer {:id 2, :notify 2}])
           ([:consumer {:id 0, :notify 0}]
            [:consumer {:id 0, :notify 1}]
            [:consumer {:id 0, :notify 2}]
            [:consumer {:id 1, :notify 0}]
            [:consumer {:id 1, :notify 1}]
            [:consumer {:id 1, :notify 2}]
            [:producer {:id 2, :notify 0}]
            [:producer {:id 2, :notify 1}]
            [:producer {:id 2, :notify 2}]))
         (model/all-models 3))))

(defn performance-shortcut
  "The last generated model finds the bug in the extreme programming challenge fourteen.
   In order to receive fast feedback, we make sure, that the last model gets checked first."
  [models]
  (reverse models))

(defn check-all [length keys]
  (reduce (fn [result model]
            (if (:property-violated result)
              (reduced result)
              (check (partial make-model
                              {START (apply all
                                            (map #(always %)
                                                 model))})
                     length
                     keys)))
          keys
          (performance-shortcut (model/all-models 3))))

(deftest ^:slow-model bounded-buffer-model-test
  (is (= {:buffer            [:X]
          :check-count       4878
          :history           '({:buffer      [:X]
                                :check-count 6
                                :occupied    0
                                :put-at      1
                                :return      :X
                                :take-at     1
                                :threads     {1 :sleeping, 2 :sleeping}}
                               {:buffer      [:X]
                                :check-count 5
                                :occupied    0
                                :put-at      1
                                :return      :X
                                :take-at     1
                                :threads     {2 :sleeping}}
                               {:buffer      [:X]
                                :check-count 4
                                :occupied    1
                                :put-at      1
                                :take-at     0
                                :threads     {1 :sleeping, 2 :sleeping}}
                               {:buffer      [:X]
                                :check-count 3
                                :occupied    1
                                :put-at      1
                                :take-at     0
                                :threads     {1 :sleeping}}
                               {:buffer      [nil]
                                :check-count 2
                                :occupied    0
                                :put-at      0
                                :take-at     0
                                :threads     {0 :sleeping, 1 :sleeping}}
                               {:buffer      [nil]
                                :check-count 1
                                :occupied    0
                                :put-at      0
                                :take-at     0
                                :threads     {1 :sleeping}}
                               {:buffer [nil], :occupied 0, :put-at 0, :take-at 0, :threads {}})
          :max-check-count   33480783
          :occupied          0
          :property-violated {:name     :there-must-be-no-deadlocks
                              :timeline [[:consumer {:id 1, :notify 0}]
                                         [:consumer {:id 0, :notify 0}]
                                         [:producer {:id 2, :notify 0}]
                                         [:producer {:id 2, :notify 1}]
                                         [:consumer {:id 0, :notify 1}]
                                         [:consumer {:id 1, :notify 1}]
                                         [:consumer {:id 0, :notify 2}]]}
          :put-at            1
          :return            :X
          :take-at           1
          :threads           {0 :sleeping, 1 :sleeping, 2 :sleeping}}
         (with-redefs [me.lomin.piggybank.bounded-buffer.spec/BUFFER-LENGTH 1]
           (check-all 7 nil)))))

(def bounded-buffer-model
  (make-model
   {START (all
           (always [:consumer {:id 0, :notify 1}])
           (always [:consumer {:id 0, :notify 2}])
           (always [:consumer {:id 1, :notify 0}])
           (always [:consumer {:id 1, :notify 2}])
           (always [:producer {:id 2, :notify 0}])
           (always [:producer {:id 2, :notify 1}]))}))

(deftest ^:unit bounded-buffer-model-retest
  (is (=  {:buffer [:X]
           :check-count 3055
           :max-check-count 1959552
           :occupied 0
           :property-violated {:name :there-must-be-no-deadlocks
                               :timeline [[:consumer {:id 1, :notify 0}]
                                          [:consumer {:id 0, :notify 2}]
                                          [:producer {:id 2, :notify 1}]
                                          [:producer {:id 2, :notify 0}]
                                          [:consumer {:id 1, :notify 0}]
                                          [:consumer {:id 1, :notify 0}]
                                          [:consumer {:id 0, :notify 1}]]}
           :put-at 1
           :take-at 1
           :threads {0 :sleeping, 1 :sleeping, 2 :sleeping}}
          (with-redefs [me.lomin.piggybank.bounded-buffer.spec/BUFFER-LENGTH 1]
            (check bounded-buffer-model
                   7
                   [:max-check-count :put-at :threads :occupied :check-count :take-at :property-violated :buffer])))))

(deftest ^:unit bounded-buffer-model-timeline-test
  (is (=* {:property-violated {:name     :there-must-be-no-deadlocks
                               :timeline [[:consumer {:id 0, :notify 0}]
                                          [:consumer {:id 1, :notify 1}]
                                          [:producer {:id 2, :notify 0}]
                                          [:producer {:id 2, :notify 2}]
                                          [:consumer {:id 0, :notify 1}]
                                          [:consumer {:id 0, :notify 2}]
                                          [:consumer {:id 1, :notify 2}]]}
           :buffer            [:X]
           :occupied          0
           :put-at            1
           :return            :X
           :take-at           1
           :threads           {2 :sleeping}}
          (with-redefs [me.lomin.piggybank.bounded-buffer.spec/BUFFER-LENGTH 1]
            (intp/interpret-timeline (spec/make-empty-universe 1)
                                     [[:consumer {:id 0, :notify 0}]
                                      [:consumer {:id 1, :notify 1}]
                                      [:producer {:id 2, :notify 0}]
                                      [:producer {:id 2, :notify 2}]
                                      [:consumer {:id 0, :notify 1}]
                                      [:consumer {:id 0, :notify 2}]
                                      [:consumer {:id 1, :notify 2}]]
                                     (partial make-model
                                              {START (all (always [:consumer {:id 0, :notify 0}])
                                                          (always [:consumer {:id 0, :notify 1}])
                                                          (always [:consumer {:id 0, :notify 2}])
                                                          (always [:consumer {:id 0, :notify 3}])
                                                          (always [:consumer {:id 1, :notify 0}])
                                                          (always [:consumer {:id 1, :notify 1}])
                                                          (always [:consumer {:id 1, :notify 2}])
                                                          (always [:consumer {:id 1, :notify 3}])
                                                          (always [:producer {:id 2, :notify 0}])
                                                          (always [:producer {:id 2, :notify 1}])
                                                          (always [:producer {:id 2, :notify 2}])
                                                          (always [:producer {:id 2, :notify 3}]))}))))))