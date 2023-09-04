(ns ollama-ui.subs
  (:require [refx.alpha :refer [reg-sub]]))

(reg-sub
 :models
 (fn [db _]
   (:models db)))
