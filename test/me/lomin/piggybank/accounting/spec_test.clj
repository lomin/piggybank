(ns me.lomin.piggybank.accounting.spec-test
  (:require [clojure.test :refer :all]
            [me.lomin.piggybank.accounting.interpreter.spec :as spec]
            [me.lomin.piggybank.checker :refer [spec-valid?]]))

(deftest ^:unit spec-test
  (spec-valid? ::spec/universe spec/empty-universe)
  (spec-valid? ::spec/universe spec/example-universe))
