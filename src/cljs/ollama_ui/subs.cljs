(ns ollama-ui.subs
  (:require [refx.alpha :refer [reg-sub]]))

;; Misc
(reg-sub
 :ollama-offline?
 (fn [db _]
   (:ollama-offline? db)))

(reg-sub
 :models
 (fn [db _]
   (:models db)))
