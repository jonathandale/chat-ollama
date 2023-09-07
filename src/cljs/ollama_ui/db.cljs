(ns ollama-ui.db
  (:require [cljs.spec.alpha :as s]))

(s/def ::ollama-offline? boolean?)

(s/def ::db (s/keys :req-un [:ollama-ui.db/ollama-offline?
                             :ollama-ui.db.model/models
                             :ollama-ui.db.model/selected-model
                             :ollama-ui.db.dialog/selected-dialog
                             :ollama-ui.db.dialog/dialogs]))

;; Default DB
(def default-db
  {:models []
   :dialogs {}
   :selected-model nil
   :selected-dialog nil
   :ollama-offline? false})
