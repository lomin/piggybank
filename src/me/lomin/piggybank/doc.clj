(ns me.lomin.piggybank.doc
  (:require [clojure.pprint :as pprint]
            [clojure.repl :as repl]
            [me.lomin.piggybank.timeline :as timeline]
            [ubergraph.alg :as ualg]
            [ubergraph.core :as ugraph]))

(def counter (atom 0))
(def ROOT "start")

(defn return-listing []
  (str "listing-" (swap! counter inc)))

(defmacro print-source [x]
  `(do (pprint/with-pprint-dispatch
         pprint/code-dispatch
         (pprint/pprint
          (clojure.edn/read-string (with-out-str (repl/source ~x)))))
       (return-listing)))

(defn print-check [chk]
  (let [result (chk)]
    (println)
    (clojure.pprint/pprint result)
    (return-listing)))

(defn print-data [data]
  (clojure.pprint/pprint data)
  (return-listing))

(defn make-node [state]
  (if-let [timeline (:timeline state)]
    (hash timeline)))

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

(defn make-state-space
  ([{:keys [model length interpreter universe prelines make-attrs]}]
   (let [prelines (vec (or (seq prelines) []))
         timelines (timeline/all-timelines-of-length length model #{prelines})]
     (reduce (partial make-graph make-attrs)
             (ugraph/add-nodes-with-attrs (ugraph/digraph) [ROOT (make-attrs universe)])
             (map (comp interpreter
                        (fn [timeline]
                          {:universe universe
                           :model    model
                           :timeline timeline}))
                  timelines)))))

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

(defn write-dot-file [g f options]
  (let [out (or (:out options) spit)]
    (do
      (out f "digraph G {\n    edge [label=0];\n    graph [ranksep=0];\n" :append false)
      (doseq [node (ugraph/nodes g)]
        (out f (str ((:make-dot-str options) g node) "\n") :append true))
      (doseq [node (ugraph/edges g)]
        (out f (str ((:make-label-dot-str options) g node) "\n") :append true))
      (str (out f "}" :append true)))))

(defn dot-string [g options]
  (write-dot-file g
                  (new StringBuffer)
                  (assoc options
                         :out
                         (fn [buffer s & _]
                           (.append buffer s)))))