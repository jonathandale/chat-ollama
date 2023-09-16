(ns ollama-ui.db.dialog
  (:require [cljs.spec.alpha :as s]))

(s/def ::prompt string?)
(s/def ::timestamp number?)
(s/def ::uuid string?)
(s/def ::model-name string?)
(s/def ::answer (s/nilable (s/map-of int? string?)))
(s/def ::exchange (s/keys :req-un [::timestamp ::prompt]
                          :opt-un [::answer]))
(s/def ::exchanges (s/nilable (s/map-of ::uuid ::exchange)))
(s/def ::dialog (s/keys :req-un [::uuid
                                 ::model-name
                                 ::timestamp]
                        :opt-un [::exchanges ::prompt]))
(s/def ::dialogs (s/nilable (s/map-of ::uuid ::dialog)))
(s/def ::selected-dialog (s/nilable ::uuid))
