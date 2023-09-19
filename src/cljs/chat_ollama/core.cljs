(ns ^:dev/once chat-ollama.core
  (:require [chat-ollama.lib :refer [defnc]]
            [helix.core :refer [$]]
            [refx.alpha :as refx :refer [dispatch-sync]]
            [chat-ollama.fx]
            [chat-ollama.events]
            [chat-ollama.subs]
            [chat-ollama.views :as views]
            ["react-dom/client" :as rdc]))

(enable-console-print!)

(dispatch-sync [:initialise-db])

(defnc root-view []
  ($ :div {:class ["h-screen" "w-screen" "bg-white" "overflow-hidden" "dark:bg-gray-900" "bg-white" "text-gray-900"]}
     ($ views/Main)))

(defonce root (rdc/createRoot (js/document.getElementById "root")))

(defn ^:dev/after-load mount-ui []
  (refx/clear-subscription-cache!)
  (.render root ($ root-view)))

(defn ^:export main []
  (mount-ui))
