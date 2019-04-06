(ns me.lomin.accounting-piggybank.spec
  (:require [clojure.spec.alpha :as s]))

(s/def ::events (s/coll-of keyword :kind set?))
(s/def ::amount int?)