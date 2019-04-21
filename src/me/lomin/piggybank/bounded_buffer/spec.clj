(ns me.lomin.piggybank.bounded-buffer.spec
  (:require [clojure.spec.alpha :as s]))

(def BUFFER-LENGTH 4)

(s/def ::universe (s/keys :req-un [::buffer ::threads ::occupied ::put-at ::take-at]))
(s/def ::occupied int?)
(s/def ::put-at int?)
(s/def ::take-at int?)
(s/def ::threads (s/map-of int? keyword?))
(s/def ::buffer (s/coll-of any? :kind vector? :max-count BUFFER-LENGTH))

(defn make-empty-universe [length]
  {:buffer   (vec (repeat length nil))
   :threads  {}
   :occupied 0
   :put-at   0
   :take-at  0})

(def empty-universe (make-empty-universe BUFFER-LENGTH))