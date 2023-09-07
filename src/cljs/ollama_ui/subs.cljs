(ns ollama-ui.subs
  (:require [refx.alpha :refer [reg-sub sub]]))

;; Misc
(reg-sub
 :ollama-offline?
 (fn [db _]
   (:ollama-offline? db)))

(reg-sub
 :models
 (fn [db _]
   (:models db)))

(reg-sub
 :selected-model
 (fn [db _]
   (:selected-model db)))

(reg-sub
 :dialogs
 (fn [db _]
   (:dialogs db)))


(reg-sub
 :model-dialogs
 (fn [_ _]
   [(sub [:selected-model])
    (sub [:dialogs])])
 (fn [[selected-model dialogs] _]
   (get dialogs selected-model)))
