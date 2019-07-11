(ns me.lomin.piggybank.accounting.doc
  (:require [me.lomin.piggybank.accounting.interpreter.core :as intp]
            [me.lomin.piggybank.accounting.interpreter.spec :as spec]
            [me.lomin.piggybank.accounting.model :as model]
            [me.lomin.piggybank.checker :as checker]
            [me.lomin.piggybank.model :refer [all
                                              always
                                              choose
                                              continue
                                              generate-incoming
                                              make-model
                                              multi-threaded
                                              then]]
            [me.lomin.piggybank.timeline :as timeline]))

(defn check
  ([model length]
   (check model length nil))
  ([model length keys]
   (checker/check {:model       model
                   :length      length
                   :keys        keys
                   :interpreter intp/interpret-timeline
                   :universe    spec/empty-universe})))

(defn accounting-events []
  [:process {:amount 1, :process-id 0} :doc "Eine Anfrage mit der Prozess-Id 0 zur Einzahlung von 1 Euro wurde gestartet."]
  [:process {:amount -1, :process-id 1} :doc "Eine Anfrage mit der Prozess-Id 1 zur Abhebung von 1 Euro wurde gestartet."]
  [:accounting-read {:amount 1, :process-id 0} :doc "Die notwendigen Daten aus dem Buchhaltungssystem wurden abgefragt. Das Ergebnis der Abfrage wurde abgelegt. Ausgelöst wurde dieses Event durch den Prozess mit der Prozess-Id 0."]
  [:accounting-write {:amount 1, :process-id 0} :doc "Die Transaktion wurde im Buchhaltungssystem festgehalten."]
  [:balance-write {:amount 1, :process-id 0} :doc "Das Saldo des Sparschweins wurde aktualisiert."])

(def accounting-subsystem {:accounting {:transfers {0 1, 1 -1}}})
(def balance-subsystem {:balance {:amount 0}})
(def event-0 [:accounting-write {:amount 1, :process-id 5}])

(defn universe-after-update []
  (let [[_ {:keys [amount process-id]}] event-0]
    (->
     (merge accounting-subsystem balance-subsystem)
     (update-in [:accounting :transfers] #(assoc % process-id amount))
     (update-in [:balance :amount] #(+ % amount)))))

(defn example-timeline-0 []
  (first (seq (timeline/all-timelines-of-length 3 model/example-model-0))))

(defn example-timeline-1 []
  (nth (seq (timeline/all-timelines-of-length 4 model/example-model-1))
       14))

(defn example-timeline-2 []
  (nth (seq (timeline/all-timelines-of-length 4 model/example-model-1))
       9))