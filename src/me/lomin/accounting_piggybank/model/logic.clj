(ns me.lomin.accounting-piggybank.model.logic)

(defmacro for-all [[sym coll] & body]
  `(every? (fn [~sym] ~@body)
           ~coll))

(defmacro there-exists [[sym coll] & body]
  `(some (fn [~sym] ~@body)
         ~coll))

(comment
  (for-all [[i j] (map (fn [num] [num (inc num)]) (range 3))]
           (prn i j)
           (and (< i 3)
                (< j 4)))

  (for-all [i (range 3)]
           (< i 2))

  (there-exists [rng #_(`IN) (map #(range % (+ % 3)) (range 100))]
                (for-all [e #_(`IN) rng]
                         (prn "->" e)
                         (< 3 e))))

