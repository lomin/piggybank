(ns me.lomin.piggybank.accounting.document-db.spec
  (:require [clojure.spec.alpha :as s]
            [me.lomin.piggybank.accounting.spec :as spec]
            [me.lomin.piggybank.spec :refer [variant?]]))

(s/def ::accounting (s/map-of variant? ::collection))
(s/def ::collection (s/or :meta-collection ::meta-collection
                          :cash-up-collection ::cash-up-collection))
(s/def ::cash-up-collection (s/map-of variant? ::cash-up-document))
(s/def ::cash-up-document (s/keys :req-un [::transfers ::next]))
(s/def ::meta-collection (s/map-of variant? ::meta-document))
(s/def ::meta-document (s/map-of variant? ::link))
(s/def ::link (s/keys :req-un [::cash-up-id ::document-id]))
(s/def ::cash-up-id some?)
(s/def ::document-id some?)
(s/def ::start ::link)
(s/def ::next ::link)
(s/def ::transfers (s/coll-of ::transfer))
(s/def ::transfer (s/tuple ::spec/processes ::spec/amount))
