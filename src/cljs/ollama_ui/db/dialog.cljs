(ns ollama-ui.db.dialog
  (:require [cljs.spec.alpha :as s]
            [ollama-ui.db.model :as model]))

(s/def ::question string?)
(s/def ::answer string?)
(s/def ::created-at string?)
(s/def ::uuid uuid?)
(s/def ::exchange (s/keys :req-un [::question ::answer]))
(s/def ::dialog (s/keys :req-un [::uuid ::created-at ::model/model] :opt-un [::exchange]))
(s/def ::model-dialogs (s/nilable (s/coll-of ::dialog :kind vector?)))
(s/def ::dialogs (s/nilable (s/map-of ::model/model ::model-dialogs)))
