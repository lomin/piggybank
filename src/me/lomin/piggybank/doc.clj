(ns me.lomin.piggybank.doc)

(def counter (atom 0))

(defn return-listing []
  (str "listing-" (swap! counter inc)))

(defmacro print-source [x]
  `(do (clojure.pprint/with-pprint-dispatch
         clojure.pprint/code-dispatch
         (clojure.pprint/pprint
          (clojure.edn/read-string (with-out-str (clojure.repl/source ~x)))))
       (return-listing)))

(defn print-check [chk]
  (let [result (chk)]
    (println)
    (clojure.pprint/pprint result)
    (return-listing)))

(defn print-data [data]
  (clojure.pprint/pprint data)
  (return-listing))