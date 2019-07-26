(ns me.lomin.piggybank.bounded-buffer.doc
  (:require [me.lomin.piggybank.bounded-buffer.interpreter.core :as intp]
            [me.lomin.piggybank.bounded-buffer.model :as model]
            [me.lomin.piggybank.bounded-buffer.spec :as spec]
            [me.lomin.piggybank.checker :as checker]
            [me.lomin.piggybank.doc :refer [print-check print-data print-source]]
            [me.lomin.piggybank.timeline :as timeline]))

(def check-keys [:max-check-count :put-at :threads :occupied :check-count :take-at :property-violated :buffer])

(defn check
  ([model length]
   (check model length nil))
  ([model length keys]
   (checker/check {:model       model
                   :length      length
                   :keys        keys
                   :interpreter intp/interpret-timeline
                   :universe    (spec/make-empty-universe spec/BUFFER-LENGTH)
                   :partitions  5})))

(defn check-with-buffer-length-1 [model length]
  (with-redefs [me.lomin.piggybank.bounded-buffer.spec/BUFFER-LENGTH 1]
    (check model length check-keys)))

(def check* (memoize check-with-buffer-length-1))

(defn bounded-buffer-events []
  [[:consumer {:id 0, :notify 0}]
   [:producer {:id 1, :notify 0}]])

(defn print-bounded-buffer-events []
  (print-source bounded-buffer-events))

(defn two-threads-model []
  (print-source me.lomin.piggybank.bounded-buffer.model/two-threads-model))

(defn check-two-threads-model []
  (print-check #(check-with-buffer-length-1 model/two-threads-model
                                            7)))
;(check* model/two-threads-model 3)
(defn bounded-buffer-universe []
  (print-data (-> (check* model/two-threads-model 3)
                  (dissoc :max-check-count :check-count))))

(defn timelines-two-threads-model []
  (print-data (timeline/all-timelines-of-length 2 model/two-threads-model)))

(defn two-producers-model []
  (print-source me.lomin.piggybank.bounded-buffer.model/two-producers-model))

(defn check-two-producers-model []
  (print-check #(check-with-buffer-length-1 model/two-producers-model
                                            7)))

(defn extreme-programming-challenge-fourteen []
  (print-source me.lomin.piggybank.bounded-buffer.model/extreme-programming-challenge-fourteen))

(defn check-extreme-programming-challenge-fourteen []
  (print-check #(check-with-buffer-length-1 model/extreme-programming-challenge-fourteen
                                            7)))