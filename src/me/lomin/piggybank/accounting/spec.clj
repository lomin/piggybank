(ns me.lomin.piggybank.accounting.spec
  (:require [clojure.spec.alpha :as s]))

(s/def ::processes (s/coll-of keyword :kind set?))
(s/def ::amount int?)