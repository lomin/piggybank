(ns me.lomin.piggybank.accounting.doc
  (:require [clojure.data.json :as json]
            [clojure.string :as string]
            [clojure.string :as str]
            [com.rpl.specter :as s]
            [me.lomin.piggybank.accounting.document-db.core :as db]
            [me.lomin.piggybank.accounting.interpreter.core :as intp]
            [me.lomin.piggybank.accounting.interpreter.spec :as spec]
            [me.lomin.piggybank.accounting.model :as model]
            [me.lomin.piggybank.checker :as checker]
            [me.lomin.piggybank.doc :refer [print-check print-data print-source]]
            [me.lomin.piggybank.model :refer [all
                                              always
                                              choose
                                              continue
                                              generate-incoming
                                              make-model
                                              multi-threaded
                                              then]]
            [me.lomin.piggybank.timeline :as timeline]
            [ubergraph.alg :as ualg]
            [ubergraph.core :as ugraph]))

(def ROOT "start")

(defn check
  ([model length]
   (check model length nil))
  ([model length keys]
   (check model length keys nil))
  ([model length keys prelines]
   (checker/check {:model       model
                   :length      length
                   :keys        keys
                   :interpreter intp/interpret-timeline
                   :universe    spec/empty-universe
                   :prelines    prelines
                   :partitions  5})))

(def check* (memoize check))

(defn example-accounting-events []
  [:process {:amount 1, :process-id 0} :doc "Eine Anfrage mit der Prozess-Id 0 zur Einzahlung von 1 Euro wurde gestartet."]
  [:process {:amount -1, :process-id 1} :doc "Eine Anfrage mit der Prozess-Id 1 zur Abhebung von 1 Euro wurde gestartet."]
  [:accounting-read {:amount 1, :process-id 0} :doc "Die notwendigen Daten aus dem Buchhaltungssystem wurden abgefragt. Das Ergebnis der Abfrage wurde abgelegt. Ausgelöst wurde dieses Event durch den Prozess mit der Prozess-Id 0."]
  [:accounting-write {:amount 1, :process-id 0} :doc "Die Transaktion wurde im Buchhaltungssystem festgehalten."]
  [:balance-write {:amount 1, :process-id 0} :doc "Das Saldo des Sparschweins wurde aktualisiert."])

(defn print-accounting-events []
  (print-source me.lomin.piggybank.accounting.doc/example-accounting-events))

(def accounting-subsystem* {:accounting {:transfers {0 1, 1 -1}}})
(def balance-subsystem* {:balance {:amount 0}})
(def universe-0* (merge accounting-subsystem* balance-subsystem*))
(def event-0* [:accounting-write {:amount 1, :process-id 5}])

(defn accounting-subsystem []
  (print-data accounting-subsystem*))
(defn balance-subsystem []
  (print-data balance-subsystem*))
(defn event-0 []
  (print-data event-0*))
(defn universe-0 []
  (print-data universe-0*))

(defn universe-after-update []
  (print-data
   (let [[_ {:keys [amount process-id]}] event-0*]
     (-> universe-0*
         (update-in [:accounting :transfers] #(assoc % process-id amount))
         (update-in [:balance :amount] #(+ % amount))))))

(defn example-timeline-0 []
  (print-data (first (seq (timeline/all-timelines-of-length 3 model/example-model-0)))))

(defn example-timeline-1 []
  (print-data (nth (seq (timeline/all-timelines-of-length 4 model/example-model-1))
                   14)))

(defn example-timeline-2 []
  (print-data (nth (seq (timeline/all-timelines-of-length 4 model/example-model-1))
                   9)))

(defn accounting-db-down-timeline []
  (print-data (nth (seq (timeline/all-timelines-of-length 3 model/multi-threaded-simple-model))
                   23)))

(defn multi-threaded-simple-model []
  (print-source me.lomin.piggybank.accounting.model/multi-threaded-simple-model))

(defn single-threaded-simple-model []
  (print-source me.lomin.piggybank.accounting.model/single-threaded-simple-model))

(defn reduced-multi-threaded-simple-model []
  (print-source me.lomin.piggybank.accounting.model/reduced-multi-threaded-simple-model))

(defn reduced-single-threaded-simple-model []
  (print-source me.lomin.piggybank.accounting.model/reduced-single-threaded-simple-model))

(defn timelines-reduced-multi-threaded-simple-model* [length]
  (timeline/all-timelines-of-length length
                                    model/reduced-multi-threaded-simple-model))

(defn timelines-reduced-multi-threaded-simple-model [length]
  (print-data (timelines-reduced-multi-threaded-simple-model* length)))

(defn count-timelines-reduced-multi-threaded-simple-model [length]
  (count (timelines-reduced-multi-threaded-simple-model* length)))

(defn timelines-reduced-single-threaded-simple-model* [length]
  (timeline/all-timelines-of-length 3
                                    model/reduced-single-threaded-simple-model))

(defn timelines-reduced-single-threaded-simple-model [length]
  (print-data (timelines-reduced-single-threaded-simple-model* length)))

(defn count-timelines-reduced-single-threaded-simple-model [length]
  (count (timelines-reduced-single-threaded-simple-model* length)))

(defn valid-sample-timeline-reduced-multi-threaded-simple-model [length]
  (print-data (last (seq (timelines-reduced-multi-threaded-simple-model* length)))))

(defn invalid-sample-timeline-reduced-multi-threaded-simple-model []
  [[:process {:amount 1, :process-id 0}]
   [:process {:amount 1, :process-id 1}]
   [:accounting-read {:amount 1, :process-id 0}]
   [:accounting-read {:amount 1, :process-id 0}]])

(defn print-invalid-sample-timeline-reduced-multi-threaded-simple-model []
  (print-source me.lomin.piggybank.accounting.doc/invalid-sample-timeline-reduced-multi-threaded-simple-model))

(def simple-model-length 7)

(defn check-multi-threaded-simple-model []
  (print-check #(-> (check* model/multi-threaded-simple-model
                            simple-model-length
                            [:check-count :max-check-count :property-violated :timeline :accounting :balance])
                    (assoc :accounting {:transfers {1 1}}))))

(defn check-single-threaded-simple-model []
  (print-check #(check* model/single-threaded-simple-model
                        simple-model-length
                        [:check-count :max-check-count :property-violated])))

(defn number-of-possible-timelines []
  (/ (:max-check-count (check* model/multi-threaded-simple-model simple-model-length nil))
     simple-model-length))

(defn number-of-checked-time-slots []
  (:check-count (check* model/multi-threaded-simple-model simple-model-length nil)))

(defn lost-updates? []
  (print-source me.lomin.piggybank.accounting.interpreter.properties/lost-updates?))

(defn single-threaded+inmemory-balance+eventually-consistent-accounting-model []
  (print-source me.lomin.piggybank.accounting.model/single-threaded+inmemory-balance+eventually-consistent-accounting-model))

(defn check-single-threaded+inmemory-balance+eventually-consistent-accounting-model []
  (print-check #(check* model/single-threaded+inmemory-balance+eventually-consistent-accounting-model
                        12
                        [:check-count :max-check-count :property-violated :timeline])))

;---

(defn get-accounting-db
  ([state]
   (let [keyvals (s/select [s/ALL (s/must :transfers) s/ALL]
                           (db/follow-next-links state))]
     (reduce (fn [result [k v]]
               (assoc result k v))
             {}
             (for [[xs value] keyvals
                   x xs]
               [x value])))))

(defn get-accounting-local
  ([state]
   (let [keyvals (s/select [(s/must :transfers) s/ALL]
                           state)]
     (reduce (fn [result [k v]]
               (assoc result k v))
             {}
             (for [[xs value] keyvals
                   x xs]
               [x value])))))

(defn get-all-process-ids [state]
  (into [] (distinct) (s/select [(s/must :timeline) s/ALL s/LAST map? (s/must :process-id)]
                                state)))

(defn get-all-local-vars [state]
  (select-keys state (get-all-process-ids state)))

(defn get-all-local-documents
  ([state]
   (apply hash-map
          (reduce into
                  []
                  (s/select [s/ALL (s/collect-one s/FIRST) s/LAST (s/must :last-document)]
                            (get-all-local-vars state))))))

(def my-hash (let [state (atom 0)]
               (memoize (fn [_] (swap! state inc)))))

(defn make-node [state]
  (if-let [timeline (:timeline state)]
    (my-hash timeline)))

(defn make-pid [process-id]
  (symbol (str "pid=" process-id)))

(defn format-time-slot [[ev {process-id :process-id :as attrs} :as time-slot]]
  (condp = ev
    :process (if (< 0 (:amount attrs))
               [(symbol "+1€") (make-pid process-id)]
               [(symbol "-1€") (make-pid process-id)])
    :balance-read [(symbol "BR") (make-pid process-id)]
    :accounting-read [(symbol "AR") (make-pid process-id)]
    :accounting-write [(symbol "AW") (make-pid process-id)]
    :balance-write [(symbol "BW") (make-pid process-id)]
    time-slot))

(defn assoc-some [m k v]
  (if v (assoc m k v) m))

(defn make-attrs [state]
  (-> {:accounting          (merge {(symbol "db") (get-accounting-db state)}
                                   (reduce-kv (fn [result k v]
                                                (assoc result k (get-accounting-local v)))
                                              {}
                                              (get-all-local-documents state)))
       :completed-transfers (vec (sort (:processes (:balance state))))
       :timeline            (:timeline state)
       :balance             (merge {(symbol "db") (or (:amount (:balance state)) 0)}
                                   (reduce-kv (fn [result k v]
                                                (assoc result k (:balance v)))
                                              {}
                                              (get-all-local-vars state)))}
      (assoc-some :property-violated (:property-violated state))
      (assoc-some :invalid-timeline (:invalid-timeline state))))

(defn check-property-violated [graph state]
  (if (:property-violated state)
    (reduced graph)
    graph))

(defn make-graph [graph state]
  (let [self (make-node state)
        previous-state (first (:history state))]
    (if-let [predecessor (make-node previous-state)]
      (-> (make-graph graph previous-state)
          (ugraph/add-nodes-with-attrs [self (make-attrs state)])
          (ugraph/add-directed-edges [predecessor self {:event (last (:timeline state))}])
          (check-property-violated state))
      (-> (ugraph/add-nodes-with-attrs graph [self (make-attrs state)])
          (ugraph/add-directed-edges [ROOT self {:event (last (:timeline state))}])
          (check-property-violated state)))))

(defn make-state-space
  ([{:keys [model length keys interpreter universe prelines]}]
   (let [prelines (vec (or (seq prelines) []))
         timelines (timeline/all-timelines-of-length length model)]
     (reductions make-graph
                 (ugraph/digraph)
                 (map (comp interpreter
                            (fn [timeline]
                              {:universe universe
                               :model    model
                               :timeline (into prelines timeline)}))
                      timelines)))))

(defn not-leaf? [g node]
  (seq (ugraph/find-edges g {:src node})))

(defn find-all-leafs [g]
  (remove (partial not-leaf? g) (ugraph/nodes g)))

(defn property-violated-node? [g node]
  false)

(defn find-node
  ([g query]
   (first (filter #(query (ugraph/attrs g %))
                  (find-all-leafs g)))))

(defn find-path [g query]
  (if-let [node (find-node g query)]
    (ualg/nodes-in-path
     (ualg/shortest-path g
                         ROOT
                         node))))

(defn remove-all-nodes [g nodes]
  (let [nodes-set (set nodes)]
    (reduce (fn [g* n] (if (nodes-set n)
                         g*
                         (ugraph/remove-nodes g* n)))
            g
            (ugraph/nodes g))))

(defn make-timeline-str [{timeline :timeline}]
  (str "{"
       (string/join "|" (cons "timeline" timeline))
       "}"))

(defn format-json [s]
  (-> (str "\\{"
           (apply str (rest (drop-last s)))
           "\\}")
      (str/replace #"\"" "")))

(defn get-table-dot-str [k v format-f state]
  (let [ks (mapv first (seq (k state)))
        vs (mapv (comp format-f
                       second) (seq (k state)))]
    (str "{" (name k) "|{process|" (str/join "|" ks) "}|{" v "|" (str/join "|" vs) "}}")))

(defn get-amount-dot-str [state]
  (str "{amount|" (or (:amount state) 0) "}"))

(defn get-completed-transfers-dot-str [state]
  (str "{completed transfers|"
       (str/join "|" (:completed-transfers state))
       "}"))

(defn make-dot-str [g node]
  (if (= node ROOT)
    (str node " [shape=record, label=\"{{accounting|{process|db}|{document|\\{\\}}}|{balance|{process|db}|{amount|0}}|{completed transfers|}}\"];")
    (let [state (ugraph/attrs g node)
          label (str \"
                     "{"
                     (get-table-dot-str :accounting
                                        "document"
                                        (comp format-json
                                              (fn [x] (json/write-str x)))
                                        state)
                     "|"
                     (get-table-dot-str :balance
                                        "amount"
                                        identity
                                        state)
                     "|"
                     (get-completed-transfers-dot-str state)
                     "}"
                     \")]
      (str node " [shape=record, label=" label "];"))))

(defn make-label-dot-str [g [src dest :as edge]]
  (str src " ->  " dest "[label=\"" (format-time-slot (:event (ugraph/attrs g edge))) "\"];"))

(defn write-dot-file
  ([g f] (write-dot-file g f {}))
  ([g f options]
   (do
     (spit f "digraph G {\n    edge [label=0];\n    graph [ranksep=0];\n" :append false)
     (doseq [node (ugraph/nodes g)]
       (spit f (str (make-dot-str g node) "\n") :append true))
     (doseq [node (ugraph/edges g)]
       (spit f (str (make-label-dot-str g node) "\n") :append true))
     (spit f "}" :append true))))

