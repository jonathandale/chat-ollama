(ns chat-ollama.db
  (:require [cljs.spec.alpha :as s]
            [chat-ollama.db.model :as model]
            [chat-ollama.db.dialog :as dialog]))

(s/def ::ollama-offline? (s/nilable boolean?))

(s/def ::db (s/keys :req-un [:chat-ollama.db/ollama-offline?
                             ::model/models
                             ::model/selected-model
                             ::dialog/dialogs
                             ::dialog/selected-dialog]))

;; Default DB
(def default-db
  {:models nil
   :dialogs nil
   :selected-model nil
   :selected-dialog nil
   :ollama-offline? nil})
