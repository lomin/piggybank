(ns me.lomin.accounting-piggybank.test-util
  (:require [clojure.data :as data]
            [clojure.test :as clojure-test]))

(defn =* [expected actual]
  (let [[things-only-in-expected _ things-in-both]
        (data/diff expected actual)]
    (if things-only-in-expected
      actual
      things-in-both)))

(defmethod clojure-test/assert-expr '=* [msg form]
  (let [[_ expected actual] form]
    (clojure-test/assert-expr msg
                              (list '=
                                    expected
                                    (list 'me.lomin.accounting-piggybank.test-util/=*
                                          expected
                                          actual)))))
