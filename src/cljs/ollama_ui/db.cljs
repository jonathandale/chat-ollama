(ns ollama-ui.db
  (:require [cljs.spec.alpha :as s]
            [ollama-ui.db.model :as model]
            [ollama-ui.db.dialog :as dialog]))

(s/def ::ollama-offline? boolean?)

(s/def ::db (s/keys :req-un [:ollama-ui.db/ollama-offline?
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
   :ollama-offline? false})
