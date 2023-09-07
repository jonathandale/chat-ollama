(ns ollama-ui.core
  (:require [ollama-ui.lib :refer [defnc]]
            [helix.core :refer [$]]
            [refx.alpha :as refx :refer [dispatch-sync]]
            [refx.http]
            [ollama-ui.fx]
            [ollama-ui.events]
            [ollama-ui.subs]
            [ollama-ui.views :as views]
            ["react-dom/client" :as rdc]))

(enable-console-print!)

(dispatch-sync [:initialise-db])

(defnc root-view []
  ($ :div {:data-tauri-drag-region true
           :class ["h-screen" "w-screen" "bg-white" "overflow-hidden" "dark:bg-gray-900"]}
     ($ views/Main)))

(defonce root (rdc/createRoot (js/document.getElementById "root")))

(defn ^:dev/after-load mount-ui []
  (refx/clear-subscription-cache!)
  (.render root ($ root-view)))

(defn ^:export main []
  (mount-ui))
