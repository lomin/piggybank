(ns me.lomin.piggybank.bounded-buffer.interpreter.core
  (:require [me.lomin.piggybank.bounded-buffer.interpreter.properties :as props]
            [me.lomin.piggybank.bounded-buffer.spec :as spec]
            [me.lomin.piggybank.interpreter :as interpreter]))

(defn update-put-at [universe]
  (update universe :put-at mod spec/BUFFER-LENGTH))

(defn insert-obj [universe]
  (assoc-in universe [:buffer (:put-at universe)] :X))

(defn sleep [universe {id :id}]
  (assoc-in universe [:threads id] :sleeping))

(defn inc-occupied [universe]
  (update universe :occupied inc))

(defn inc-put-at [universe]
  (update universe :put-at inc))

(defn dec-occupied [universe]
  (update universe :occupied dec))

(defn update-take-at [universe]
  (update universe :take-at mod spec/BUFFER-LENGTH))

(defn return-obj [universe]
  (assoc universe :return (nth (:buffer universe) (:take-at universe))))

(defn inc-take-at [universe]
  (update universe :take-at inc))

(defn notify [universe {notify-id :notify}]
  (if (or (get-in universe [:threads notify-id])
          (empty? (:threads universe)))
    (update universe :threads dissoc notify-id)
    (assoc universe :invalid-timeline true)))

(defn consume [universe data]
  (if (= (:occupied universe) 0)
    (sleep universe data)
    (-> universe
        (notify data)
        (dec-occupied)
        (update-take-at)
        (return-obj)
        (inc-take-at))))

(defn produce [universe data]
  (if (= (:occupied universe) spec/BUFFER-LENGTH)
    (sleep universe data)
    (-> universe
        (notify data)
        (inc-occupied)
        (update-put-at)
        (insert-obj)
        (inc-put-at))))

(defn sleeping? [universe {id :id}]
  (= :sleeping (get-in universe [:threads id])))

(defn interpret-event [universe [event-type data]]
  (if (sleeping? universe data)
    universe
    (condp = event-type
      :consumer (consume universe data)
      :producer (produce universe data)
      universe)))

(def SPECIFICS {:interpret-event        interpret-event
                :any-property-violation props/any-property-violation})

(defn interpret-timeline
  ([context]
   (interpreter/interpret-timeline (merge context SPECIFICS)))
  ([state timeline model]
   (interpret-timeline nil state timeline model))
  ([progress-bar state timeline model]
   (interpret-timeline {:progress-bar progress-bar
                        :universe     state
                        :timeline     timeline
                        :model        model
                        :interpreter  interpret-timeline})))
