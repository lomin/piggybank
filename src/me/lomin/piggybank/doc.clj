(ns me.lomin.piggybank.doc
  (:require [clojure.pprint :as pprint]
            [clojure.repl :as repl]))

(def counter (atom 0))

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