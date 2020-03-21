(ns me.lomin.piggybank.state-space
  (:require [me.lomin.piggybank.timeline :as timeline]
            [ubergraph.alg :as ualg]
            [ubergraph.core :as ugraph]))

(def ROOT "start")

(def my-hash (let [state (atom 0)]
               (memoize (fn [_] (swap! state inc)))))

(defn make-node [state]
  (if-let [timeline (:timeline state)]
    (my-hash timeline)))

(defn check-property-violated [graph state]
  (if (:property-violated state)
    (reduced graph)
    graph))

(defn make-graph [make-attrs graph state]
  (let [self (make-node state)
        previous-state (first (:history state))]
    (if-let [predecessor (make-node previous-state)]
      (-> (make-graph make-attrs graph previous-state)
          (ugraph/add-nodes-with-attrs [self (make-attrs state)])
          (ugraph/add-directed-edges [predecessor self {:event (last (:timeline state))}])
          (check-property-violated state))
      (-> (ugraph/add-nodes-with-attrs graph [self (make-attrs state)])
          (ugraph/add-directed-edges [ROOT self {:event (last (:timeline state))}])
          (check-property-violated state)))))

(defn create [{:keys [model length interpreter universe prelines make-attrs]}]
  (let [prelines (vec (or (seq prelines) []))
        timelines (timeline/all-timelines-of-length length model #{prelines})]
    (reduce (partial make-graph make-attrs)
            (ugraph/add-nodes-with-attrs (ugraph/digraph) [ROOT (make-attrs universe)])
            (map (comp interpreter
                       (fn [timeline]
                         {:universe universe
                          :model    model
                          :timeline timeline}))
                 timelines))))

(defn not-leaf? [g node]
  (seq (ugraph/find-edges g {:src node})))

(defn find-all-leafs [g]
  (remove (partial not-leaf? g) (ugraph/nodes g)))

(defn find-node
  ([g query]
   (first (filter #(query (ugraph/attrs g %))
                  (find-all-leafs g)))))

(defn get-path [g node]
  (ualg/nodes-in-path
   (ualg/shortest-path g
                       ROOT
                       node)))

(defn find-path [g query]
  (if-let [node (find-node g query)]
    (get-path g node)))

(defn remove-all-nodes [g nodes]
  (let [nodes-set (set nodes)]
    (reduce (fn [g* n] (if (nodes-set n)
                         g*
                         (ugraph/remove-nodes g* n)))
            g
            (ugraph/nodes g))))
