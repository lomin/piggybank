(ns me.lomin.accounting-piggybank.spec-test
  (:require [clojure.spec.alpha :as s]
            [clojure.test :refer :all]
            [me.lomin.accounting-piggybank.interpreter.spec :as spec]))

(defn spec-valid? [spec x]
  (is (s/valid? spec x)
      (if (not (s/valid? spec x))
        (s/explain spec x))))

(deftest spec-test
  (spec-valid? ::spec/context spec/example-context))
