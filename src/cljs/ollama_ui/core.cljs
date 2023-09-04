(ns ollama-ui.core
  (:require [ollama-ui.lib :refer [defnc]]
            [helix.core :refer [$]]
            [refx.alpha :as refx :refer [dispatch-sync]]
            [refx.http]
            ["react-dom/client" :as rdc]
            [ollama-ui.events]
            [ollama-ui.subs]
            [ollama-ui.views :as views]))

(enable-console-print!)

(dispatch-sync [:initialise-db])

(defnc root-view []
  ($ :div {:data-tauri-drag-region true
           :class ["h-screen" "w-screen" "bg-white" "dark:bg-gray-900" "overflow-hidden"]}
     ($ views/Main)))

(defonce root (rdc/createRoot (js/document.getElementById "root")))

(defn ^:dev/after-load mount-ui []
  (refx/clear-subscription-cache!)
  (.render root ($ root-view)))

(defn ^:export main []
  (mount-ui))
