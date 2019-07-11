(ns me.lomin.piggybank.bounded-buffer.interpreter-test
  (:require [clojure.test :refer :all]
            [me.lomin.piggybank.asserts :refer [=*]]
            [me.lomin.piggybank.bounded-buffer.interpreter.core :as intp]
            [me.lomin.piggybank.bounded-buffer.interpreter.properties :as props]
            [me.lomin.piggybank.bounded-buffer.model :as model]
            [me.lomin.piggybank.bounded-buffer.spec :as spec]
            [me.lomin.piggybank.model :refer [all always make-model]]))

(defn interpret-timeline [universe timeline]
  (intp/interpret-timeline universe timeline model/simple-bounded-buffer-model))

(deftest ^:unit interpret-producer-test
  (is (= {:buffer      [:X nil nil nil]
          :check-count 1
          :history     '({:buffer   [nil nil nil nil]
                          :occupied 0
                          :put-at   0
                          :take-at  0
                          :threads  {}})
          :occupied    1
          :put-at      1
          :take-at     0
          :threads     {}}
         (interpret-timeline spec/empty-universe
                             [[:producer {:id 0 :notify 0}]])))

  (testing "do nothing if I'm sleeping"
    (let [universe (update spec/empty-universe :threads assoc 0 :sleeping)]
      (is (=* {:buffer      [nil nil nil nil]
               :check-count 1
               :occupied    0
               :put-at      0}
              (interpret-timeline universe
                                  [[:producer {:id 0 :notify 0}]])))))

  (testing "sleep when occupied == buffer-length"
    (is (=* {:buffer   [:X :X :X :X]
             :occupied 4
             :put-at   4
             :threads  {0 :sleeping}}
            (interpret-timeline spec/empty-universe
                                [[:producer {:id 0 :notify 0}]
                                 [:producer {:id 0 :notify 0}]
                                 [:producer {:id 0 :notify 0}]
                                 [:producer {:id 0 :notify 0}]
                                 [:producer {:id 0 :notify 0}]
                                 [:producer {:id 0 :notify 0}]
                                 [:producer {:id 0 :notify 0}]])))

    (is (=* {:buffer   [:X :X :X :X]
             :occupied 4
             :put-at   4
             :threads  {4 :sleeping}}
            (interpret-timeline spec/empty-universe
                                [[:producer {:id 0 :notify 0}]
                                 [:producer {:id 1 :notify 1}]
                                 [:producer {:id 2 :notify 2}]
                                 [:producer {:id 3 :notify 3}]
                                 [:producer {:id 4 :notify 4}]
                                 [:producer {:id 4 :notify 4}]
                                 [:producer {:id 4 :notify 4}]])))))

(deftest ^:unit interpret-consumer-test
  (is (= {:buffer      [nil nil nil nil]
          :check-count 1
          :history     [{:buffer   [nil nil nil nil]
                         :occupied 0
                         :put-at   0
                         :take-at  0
                         :threads  {}}]
          :occupied    0
          :put-at      0
          :take-at     0
          :threads     {0 :sleeping}}
         (interpret-timeline spec/empty-universe
                             [[:consumer {:id 0 :notify 0}]])))

  (testing "do nothing if I'm sleeping"
    (let [universe (update spec/empty-universe :threads assoc 0 :sleeping)]
      (is (=* {:buffer      [nil nil nil nil]
               :check-count 1
               :occupied    0
               :put-at      0
               :take-at     0
               :threads     {0 :sleeping}}
              (interpret-timeline universe
                                  [[:consumer {:id 0 :notify 0}]])))))

  (testing "sleep when occupied == buffer-length"
    (is (=* {:buffer      [:X :X nil nil]
             :check-count 7
             :occupied    0
             :put-at      2
             :return      :X
             :take-at     2
             :threads     {0 :sleeping}}
            (interpret-timeline spec/empty-universe
                                [[:producer {:id 0 :notify 0}]
                                 [:consumer {:id 0 :notify 0}]
                                 [:producer {:id 0 :notify 0}]
                                 [:consumer {:id 0 :notify 0}]
                                 [:consumer {:id 0 :notify 0}]
                                 [:consumer {:id 0 :notify 0}]
                                 [:consumer {:id 0 :notify 0}]])))))