(ns ollama-ui.db
  (:require [cljs.spec.alpha :as s]))

;; MODELS
(s/def ::digest string?)
(s/def ::size int?)
(s/def ::name string?)

(s/def ::model (s/keys :req-un [::digest ::name ::size]))
(s/def ::models (s/nilable (s/coll-of ::model :kind vector?)))

;; DIALOGS
;; (s/def ::dialog-question string?)
;; (s/def ::dialog-answer string?)
;; (s/def ::dialog-created-at string?)
;; (s/def ::dialog-uuid uuid?)
;; (s/def ::dialog-exchange (s/keys :req-un [::dialog-question ::dialog-answer]))
;; (s/def ::dialog (s/keys :req-un [::dialog-uuid ::dialog-created-at ::model-name ::dialog-exchange]))
;; (s/def ::dialogs (s/nilable (s/coll-of ::dialog :kind [])))

;; ;; UI
;; (s/def ::selected-dialog (s/nilable uuid?))

(s/def ::db (s/keys :req-un [::models]))

;; Default DB
(def default-db
  {:models nil})
