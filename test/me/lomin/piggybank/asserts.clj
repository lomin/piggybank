(ns me.lomin.piggybank.asserts
  (:require [clojure.core.reducers :as r]
            [clojure.data :as data]
            [clojure.spec.alpha :as s]
            [clojure.test :as test]
            [clojure.test :as clojure-test]))

(defn spec-invalid? [spec x]
  (test/is (not (s/valid? spec x))
           (if (s/valid? spec x)
             (s/explain spec x))))

(defn spec-valid? [spec x]
  (test/is (s/valid? spec x)
           (if (not (s/valid? spec x))
             (s/explain spec x))))

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
                                    (list 'me.lomin.piggybank.asserts/=*
                                          expected
                                          actual)))))