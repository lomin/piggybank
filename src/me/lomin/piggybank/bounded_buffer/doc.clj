(ns me.lomin.piggybank.bounded-buffer.doc
  (:require [clojure.string :as str]
            [me.lomin.piggybank.bounded-buffer.interpreter.core :as intp]
            [me.lomin.piggybank.bounded-buffer.model :as model]
            [me.lomin.piggybank.bounded-buffer.spec :as spec]
            [me.lomin.piggybank.checker :as checker]
            [me.lomin.piggybank.doc :refer [print-check print-data print-source]]
            [me.lomin.piggybank.doc :as doc]
            [me.lomin.piggybank.timeline :as timeline]
            [ubergraph.core :as ugraph]))

(def check-keys [:max-check-count :put-at :threads :occupied :check-count :take-at :property-violated :buffer])

(defn check
  ([model length]
   (check model length nil))
  ([model length keys]
   (checker/check {:model       model
                   :length      length
                   :keys        keys
                   :interpreter intp/interpret-timeline
                   :universe    (spec/make-empty-universe spec/BUFFER-LENGTH)
                   :partitions  5})))

(defn check-with-buffer-length-1 [model length]
  (with-redefs [me.lomin.piggybank.bounded-buffer.spec/BUFFER-LENGTH 1]
    (check model length check-keys)))

(def check* (memoize check-with-buffer-length-1))

(defn bounded-buffer-events []
  [[:consumer {:id 0, :notify 0}]
   [:producer {:id 1, :notify 0}]])

(defn print-bounded-buffer-events []
  (print-source bounded-buffer-events))

(defn two-threads-model []
  (print-source me.lomin.piggybank.bounded-buffer.model/two-threads-model))

(defn check-two-threads-model []
  (print-check #(check-with-buffer-length-1 model/two-threads-model
                                            7)))
;(check* model/two-threads-model 3)
(defn bounded-buffer-universe []
  (print-data (-> (check* model/two-threads-model 3)
                  (dissoc :max-check-count :check-count))))

(defn timelines-two-threads-model []
  (print-data (timeline/all-timelines-of-length 2 model/two-threads-model)))

(defn two-producers-model []
  (print-source me.lomin.piggybank.bounded-buffer.model/two-producers-model))

(defn check-two-producers-model []
  (print-check #(check-with-buffer-length-1 model/two-producers-model
                                            7)))

(defn extreme-programming-challenge-fourteen []
  (print-source me.lomin.piggybank.bounded-buffer.model/extreme-programming-challenge-fourteen))

(defn check-extreme-programming-challenge-fourteen []
  (print-check #(check-with-buffer-length-1 model/extreme-programming-challenge-fourteen
                                            7)))

(defn make-attrs [threads node]
  {:property-violated (:property-violated node)
   :timeline          (:timeline node)
   :buffer            (if (every? nil? (:buffer node))
                        "empty"
                        "full")
   :occupied          (:occupied node)
   :put-at            (:put-at node)
   :take-at           (:take-at node)
   :threads           (reduce (fn [m [kind {id :id}]]
                                (assoc m id [kind (if (get-in node [:threads id])
                                                    "sleeping"
                                                    "awake")]))
                              {}
                              threads)})

(defn edn->dot-str [k state]
  (str "{" (name k) "|" (k state) "}"))

(defn write-dot-threads [state]
  "{threads|{id|0|1}|{kind|consumer|producer}|{state|sleeping|awake}}|{occupied|0}"
  (let [ids (mapv first (seq (:threads state)))
        kinds (mapv (comp name first second) (seq (:threads state)))
        status (mapv (comp second second) (seq (:threads state)))]
    (str "{threads|{id|"
         (str/join "|" ids)
         "}|{kind|"
         (str/join "|" kinds)
         "}|{state|"
         (str/join "|" status)
         "}}")))

(defn make-dot-str [g node]
  (let [state (ugraph/attrs g node)
        label (str \"
                   "{"
                   (edn->dot-str :buffer state)
                   "|"
                   (write-dot-threads state)
                   "|"
                   (edn->dot-str :occupied state)
                   "|"
                   (edn->dot-str :put-at state)
                   "|"
                   (edn->dot-str :take-at state)
                   "}"
                   \")]
    (str node " [shape=record, label=" label "];")))

(defn format-time-slot [node [_ {id :id notify :notify} :as ev]]
  (let [[_ status] (get-in node [:threads notify])]
    (if (= status "sleeping")
      (str id "->notify:" notify)
      (str id))))

(defn make-label-dot-str [g [src dest :as edge]]
  (str src " ->  " dest "[label=\"" (format-time-slot (ugraph/attrs g src) (:event (ugraph/attrs g edge))) "\"];"))

(defn write-dot-file
  ([g f] (doc/write-dot-file g f {:make-dot-str       make-dot-str
                                  :make-label-dot-str make-label-dot-str})))

(defn get-bad-state [g]
  (let [node (last (doc/find-path g :property-violated))
        node+attrs (ugraph/attrs g node)
        format-events (fn [events]
                        (mapv format-time-slot
                              (mapv (partial ugraph/attrs g) (doc/get-path g node))
                              events))]
    (-> node+attrs
        (update :timeline format-events)
        (assoc :threads (reduce-kv (fn [m k [kind status]]
                                     (assoc m (str (name kind) "-" k) status))
                                   {}
                                   (:threads node+attrs)))
        (assoc :property-violated (get-in node+attrs [:property-violated :name])))))

(defn write-bad-state [g]
  (spit "/tmp/bad-state.json" (clojure.data.json/write-str (get-bad-state g))))