(ns ollama-ui.db.model
  (:require [cljs.spec.alpha :as s]))

(s/def ::digest string?)
(s/def ::size int?)
(s/def ::name string?)

(s/def ::model (s/keys :req-un [::digest ::name ::size]))
(s/def ::models (s/nilable (s/coll-of ::model :kind coll?)))
(s/def ::selected-model (s/nilable ::name))
