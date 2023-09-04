(ns ollama-ui.db.dialog
  (:require [cljs.spec.alpha :as s]))

(s/def ::question string?)
(s/def ::answer string?)
(s/def ::created-at string?)
(s/def ::uuid uuid?)
(s/def ::exchange (s/keys :req-un [::question ::answer]))
(s/def ::dialog (s/keys :req-un [::uuid ::created-at ::model-name ::exchange]))
(s/def ::dialogs (s/nilable (s/coll-of ::dialog :kind [])))

;; UI
(s/def ::selected-dialog (s/nilable uuid?))
