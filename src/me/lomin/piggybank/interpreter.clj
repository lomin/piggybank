(ns me.lomin.piggybank.interpreter)

(def inc-or-0 (fnil inc 0))

(def add (fnil conj []))

(defn inc-check-count [state progress-bar]
  (when progress-bar (progress-bar))
  (update state :check-count inc-or-0))

(defn add-previous-state [state previous-state]
  (update state :history (fnil conj (list)) previous-state))

(defn add-property-violation [state violation timeline]
  (assoc state :property-violated {:name violation :timeline timeline}))

(defn check-properties [universe {:keys [timeline any-property-violation progress-bar] :as context}]
  (if-let [violation (any-property-violation (assoc context :universe universe))]
    (reduced (add-property-violation universe violation timeline))
    (inc-check-count universe progress-bar)))

(defn interpret-timeline [{:keys [interpret-event universe timeline progress-bar] :as context}]
  (reduce (fn [universe* event]
            (let [successor-universe (-> universe*
                                         (interpret-event event)
                                         (add-previous-state universe*)
                                         (update :timeline add event))]
              (if (:invalid-timeline successor-universe)
                (inc-check-count (assoc universe* :invalid-timeline true) progress-bar)
                (check-properties successor-universe
                                  (assoc context :event event)))))
          universe
          timeline))
