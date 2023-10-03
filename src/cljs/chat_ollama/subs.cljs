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
 :dialog-list
 :<- [:dialogs]
 (fn [dialogs]
   (->> dialogs
        (vals)
        (map #(dissoc % :exchanges))
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
 :dialog-meta
 :<- [:dialogs]
 (fn [dialogs [_ dialog-uuid]]
   (-> (get dialogs dialog-uuid)
       (select-keys [:title :model-name]))))

(reg-sub
 :dialog-exchanges
 :<- [:dialogs]
 (fn [dialogs [_ dialog-uuid]]
   (->> (get-in dialogs [dialog-uuid :exchanges])
        (map (fn [[k v]]
               (assoc v :uuid k)))
        (sort-by :timestamp)
        (mapv :uuid))))

(reg-sub
 :dialog-exchange
 (fn [db [_ {:keys [dialog-uuid exchange-uuid]}]]
   (get-in db [:dialogs dialog-uuid :exchanges exchange-uuid])))
