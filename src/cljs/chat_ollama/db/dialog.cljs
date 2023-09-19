(ns chat-ollama.db.dialog
  (:require [cljs.spec.alpha :as s]))

(s/def ::prompt string?)
(s/def ::timestamp number?)
(s/def ::uuid string?)
(s/def ::model-name string?)
(s/def ::answer (s/nilable string?))


(s/def ::context coll?)
(s/def ::created_at string?)
(s/def ::eval_count int?)
(s/def ::eval_duration int?)
(s/def ::load_duration int?)
(s/def ::prompt_eval_count int?)
(s/def ::prompt_eval_duration int?)
(s/def ::total_duration int?)
(s/def ::response string?)
(s/def ::meta (s/keys :req-un [::context
                               ::created_at
                               ::eval_count
                               ::eval_duration
                               ::load_duration
                               ::prompt_eval_count
                               ::total_duration
                               ::response]
                      :opt-un [::prompt_eval_duration]))

(s/def ::exchange (s/keys :req-un [::timestamp ::prompt]
                          :opt-un [::answer ::meta]))
(s/def ::exchanges (s/nilable (s/map-of ::uuid ::exchange)))
(s/def ::dialog (s/keys :req-un [::uuid
                                 ::model-name
                                 ::timestamp]
                        :opt-un [::exchanges ::prompt]))
(s/def ::dialogs (s/nilable (s/map-of ::uuid ::dialog)))
(s/def ::selected-dialog (s/nilable ::uuid))
