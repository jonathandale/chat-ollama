(ns ollama-ui.db.dialog
  (:require [cljs.spec.alpha :as s]
            [ollama-ui.db.model :as model]))

(s/def ::question string?)
(s/def ::answer string?)
(s/def ::created-at string?)
(s/def ::uuid string?)
(s/def ::exchange (s/keys :req-un [::question ::answer]))
(s/def ::dialog (s/keys :req-un [::uuid ::created-at ::model/name] :opt-un [::exchange]))
(s/def ::model-dialogs (s/nilable (s/coll-of ::dialog :kind coll?)))
(s/def ::dialogs (s/nilable (s/map-of ::model/name ::model-dialogs)))
(s/def ::selected-dialog (s/nilable ::uuid))
