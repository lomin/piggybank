(ns me.lomin.piggybank.bounded-buffer.spec-test
  (:require [clojure.test :refer :all]
            [me.lomin.piggybank.bounded-buffer.spec :as spec]
            [me.lomin.piggybank.checker :refer [spec-invalid? spec-valid?]]))

(deftest ^:unit bounded-buffer-spec-test
  (spec-valid? ::spec/universe spec/empty-universe)
  (spec-valid? ::spec/universe (assoc spec/empty-universe :buffer [1 2]))
  (spec-invalid? ::spec/universe (assoc spec/empty-universe :buffer [1 2 3 4 5 6 7 8 9]))

  (spec-valid? ::spec/universe (assoc spec/empty-universe :threads {1 :sleeping})))