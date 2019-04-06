(ns me.lomin.accounting-piggybank.accounting.spec
  (:require [clojure.spec.alpha :as s]
            [me.lomin.accounting-piggybank.spec :as spec]))

(s/def ::accounting (s/map-of keyword? ::collection))
(s/def ::collection (s/or :meta-collection ::meta-collection
                          :branch-collection ::branch-collection))
(s/def ::branch-collection (s/map-of keyword? ::branch-document))
(s/def ::branch-document (s/keys :req-un [::data ::next]))
(s/def ::meta-collection (s/keys :req-un [::meta-document]))
(s/def ::meta-document (s/and (s/keys :req-un [::first])
                              (s/map-of keyword? ::link)))
(s/def ::link (s/tuple keyword? keyword?))
(s/def ::first ::link)
(s/def ::next ::link)
(s/def ::data (s/coll-of ::value))
(s/def ::value (s/tuple ::spec/events ::spec/val))
