(ns ollama-ui.db
  (:require [cljs.spec.alpha :as s]))

(s/def ::ollama-offline? boolean?)

(s/def ::db (s/keys :req-un [:ollama-ui.db/ollama-offline?
                             :ollama-ui.db.model/models
                             :ollama-ui.db.dialog/dialogs
                             :ollama-ui.db.dialog/selected-dialog]))

;; Default DB
(def default-db
  {:models nil
   :dialogs nil
   :selected-dialog nil
   :ollama-offline? false})
