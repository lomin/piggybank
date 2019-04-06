(ns me.lomin.accounting-piggybank.accounting.core)

(defn get-document-by-link [state link]
  (get-in (:accounting state) link))

(defn overwrite-document-by-link [state link document]
  (assoc-in state (into [:accounting] link) document))

(defn get-start-document [state]
  (get-document-by-link state
                        (get-in state
                                [:accounting :meta :meta-document :first])))

(defn follow-next-links
  ([state]
   (follow-next-links state (get-start-document state)))
  ([state last-document]
   (lazy-seq
    (cons last-document
          (let [next-document (get-document-by-link state (:next last-document))]
            (if (or (nil? next-document)
                    (= next-document last-document))
              nil
              (follow-next-links state next-document)))))))

(defn get-last-document [state]
  (last (follow-next-links state)))

(defn add-counter-value [document k value]
  (update document :data conj [#{k} value]))

(defn id->key [id]
  (keyword (str id)))

(defn make-branch-init-link [{k :key}]
  [k (id->key (str (name k) "-init"))])

(defn make-new-document [link data]
  {:next link :self link :data data})

(defn insert-new-document [state link & data]
  (overwrite-document-by-link state
                              link
                              (make-new-document link
                                                 (vec data))))