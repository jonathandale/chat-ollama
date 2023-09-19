(ns chat-ollama.subs
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
   (->> dialogs
        (vals)
        (filter (fn [dialog]
                  (= selected-model (:name dialog))))
        (sort-by :timestamp)
        (reverse))))

(reg-sub
 :selected-dialog
 (fn [db _]
   (:selected-dialog db)))

(reg-sub
 :dialog
 :<- [:dialogs]
 (fn [dialogs [_ dialog-uuid]]
   (get dialogs dialog-uuid)))


(reg-sub
 :dialog-exchanges
 (fn [_ _]
   [(sub [:selected-dialog])
    (sub [:dialogs])])
 (fn [[selected-dialog dialogs] _]
   (as-> dialogs $
     (get-in $ [selected-dialog :exchanges])
     (vals $)
     (sort-by :timestamp $))))
