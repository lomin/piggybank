(ns me.lomin.piggybank.spec)

(defn variant? [x]
  (and (vector? x) (keyword? (first x))))
