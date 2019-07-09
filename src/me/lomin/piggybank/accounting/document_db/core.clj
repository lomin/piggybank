(ns me.lomin.piggybank.accounting.document-db.core)

(def META-LINK {:cash-up-id :meta :document-id :meta})

(defn- link->selector [{cash-up-id :cash-up-id document-id :document-id}]
  [[:cash-up cash-up-id] [:document document-id]])

(defn make-link [cash-up-id document-id]
  {:cash-up-id cash-up-id :document-id document-id})

(defn get-document-by-link [state link]
  (get-in (:accounting state) (link->selector link)))

(defn overwrite-document-by-link [state link document]
  (assoc-in state (into [:accounting] (link->selector link))
            document))

(defn get-meta-document [state]
  (get-document-by-link state META-LINK))

(defn get-meta-start-link [state]
  (get (get-meta-document state)
       [:cash-up :start]))

(defn get-start-document [state]
  (get-document-by-link state (get-meta-start-link state)))

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
  (update document :transfers conj [#{k} value]))

(defn make-branch-init-link [{process-id :process-id}]
  {:cash-up-id process-id :document-id (str process-id "-init")})

(defn make-new-document [link data]
  {:next link :self link :transfers data})

(defn insert-new-document [state link & data]
  (overwrite-document-by-link state
                              link
                              (make-new-document link
                                                 (vec data))))

(defn insert-branch-in-meta [state {cash-up-id :cash-up-id :as link}]
  (overwrite-document-by-link state
                              META-LINK
                              (-> state
                                  (get-meta-document)
                                  (assoc [:cash-up :start] link)
                                  (assoc [:cash-up cash-up-id] link))))