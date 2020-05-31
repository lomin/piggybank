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
            [me.lomin.piggybank.doc :refer [print-check print-data print-source] :as doc]
            [me.lomin.piggybank.model :refer [all
                                              always
                                              choose
                                              continue
                                              generate-incoming
                                              make-model
                                              multi-threaded
                                              then]]
            [me.lomin.piggybank.timeline :as timeline]
            [ubergraph.core :as ugraph]))

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
  [:terminate/balance-write {:amount 1, :process-id 0} :doc "Das Saldo des Sparschweins wurde aktualisiert."])

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

(defn make-pid [process-id]
  (symbol (str "pid=" process-id)))

(defn format-reset [[_ {steps :go-steps-back-in-timeline} :as time-slot]]
  [(symbol (str "RW" steps))])

(defn format-time-slot [[ev {process-id :process-id :as attrs} :as time-slot]]
  (condp = ev
    :process (if (< 0 (:amount attrs))
               [(symbol "+1€") (make-pid process-id)]
               [(symbol "-1€") (make-pid process-id)])
    :balance-read [(symbol "BR") (make-pid process-id)]
    :accounting-read [(symbol "AR") (make-pid process-id)]
    :accounting-write [(symbol "AW") (make-pid process-id)]
    :terminate/balance-write [(symbol "BW") (make-pid process-id)]
    :accounting-read-last-write [(symbol "ALWR") (make-pid process-id)]
    :terminate/restart (format-reset time-slot)
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

(defn node-label [s]
  (clojure.walk/postwalk
   (fn [e]
     (cond
       (keyword? e) (name e)
       (map-entry? e) e
       (map? e) (-> (json/write-str e)
                    (string/replace #"\\" "")
                    (string/replace #"\{" "\\\\{")
                    (string/replace #"\}" "\\\\}")
                    (str/replace #"\"" ""))
       (vector? e) (str "{" (string/join "|" e) "}")
       :else (str e)))
   s))

(defn escape [& strs]
  (str \" (string/join strs) \"))

(defn string-node [node label]
  (str node " [shape=record, label=" (escape (node-label label)) "];"))

(defn state->node
  ([state k]
   "for vector state"
   (if-let [xs (seq (get state k))]
     (into [k] xs)
     [k ""]))
  ([state k header]
   "for map state"
   (let [sq (cons header (seq (k state)))
         ks (mapv first sq)
         vs (mapv second sq)]
     [k ks vs])))

(defn make-dot-str [g node]
  (if (= node doc/ROOT)
    (string-node node [["accounting" ["process" "db"] ["document" {}]]
                       ["balance" ["process" "db"] ["amount" 0]]
                       ["completed transfers" ""]])
    (let [state (ugraph/attrs g node)]
      (string-node node
                   (reduce conj
                           []
                           [(state->node state :accounting [:process :document])
                            (state->node state :balance [:process :amount])
                            (state->node state :completed-transfers)])))))

(defn make-label-dot-str [g [src dest :as edge]]
  (str src " ->  " dest "[label=\"" (format-time-slot (:event (ugraph/attrs g edge))) "\"];"))

(defn write-dot-file
  ([g] (doc/dot-string g {:make-dot-str       make-dot-str
                          :make-label-dot-str make-label-dot-str}))
  ([g $file] (doc/write-dot-file g $file {:make-dot-str       make-dot-str
                                          :make-label-dot-str make-label-dot-str})))

(defn get-bad-state [g]
  (let [format-events (fn [events]
                        (mapv (comp (partial str/join " ")
                                    format-time-slot) events))
        node (ugraph/attrs g (last (doc/find-path g :property-violated)))]
    (-> node
        (update :timeline format-events)
        (assoc :property-violated (get-in node [:property-violated :name])))))