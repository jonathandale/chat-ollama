(ns ollama-ui.db.dialog
  (:require [cljs.spec.alpha :as s]
            [ollama-ui.db.model :as model]))

(s/def ::question string?)
(s/def ::answer string?)
(s/def ::created-at string?)
(s/def ::timestamp number?)
(s/def ::uuid string?)
(s/def ::exchange (s/keys :req-un [::question]
                          :opt-un [::answer]))
(s/def ::dialog (s/keys :req-un [::uuid
                                 ::created-at
                                 ::model/name
                                 ::timestamp]
                        :opt-un [::exchange]))
(s/def ::dialogs (s/nilable (s/map-of ::uuid ::dialog)))
(s/def ::selected-dialog (s/nilable ::uuid))
