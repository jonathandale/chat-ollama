(ns ollama-ui.views
  (:require [clojure.string :as str]
            [clojure.set :refer [union]]
            [ollama-ui.lib :refer [defnc]]
            [helix.core :refer [$ <>]]
            [helix.hooks :refer [use-effect]]
            [refx.alpha :refer [use-sub dispatch]]))

(defn- b->gb [bytes]
  (.toFixed (/ bytes 1024 1024 1024) 2))

(defnc Header []
  (let [models (use-sub [:models])
        selected-model (use-sub [:selected-model])]
    ($ :div {:data-tauri-drag-region true
             :class ["flex" "items-center" "justify-center" "text-white" "py-4"]}
       (when (seq models)
         ($ :div {:class ["flex" "gap-3"]}
            (for [model models]
              (let [[model-name model-version] (str/split (:name model) #":")
                    class #{"bg-gray-800/80" "hover:bg-gray-800"}
                    selected-class #{"bg-white" "text-gray-900"}]
                ($ :button {:key (:digest model)
                            :class (vec (union #{"rounded-full" "px-4" "py-1.5" "text-sm"}
                                               (if (= (:name selected-model) (:name model))
                                                 selected-class
                                                 class)))
                            :on-click #(dispatch [:set-selected-model model])}
                   model-name
                   ($ :span {:class ["opacity-50"]} ":" model-version)))))))))

(defnc Dialog []
  ($ :div {:data-tauri-drag-region true
           :class ["grow" "w-full" "max-w-md" "mx-auto"]}
     ($ :p "Dialog")))

(defnc Footer []
  ($ :div {:class ["max-w-md" "w-full" "mx-auto"]}
     ($ :p "Footer")))

(defnc Offline []
  ($ :div {:data-tauri-drag-region true
           :class ["flex" "flex-col" "grow" "w-full" "justify-center" "items-center"]}
     ($ :img {:class ["w-20" "h-auto" "mb-10" "pointer-events-none"]
              :src "/assets/ollama-asleep.svg"
              :alt "Looks like Ollama is Offline"})
     ($ :h1 {:class ["text-white" "text-3xl" "select-none" "pointer-events-none"]}
        "Looks like Ollama is asleep!")
     ($ :h2 {:class ["text-lg" "text-gray-500" "mb-10" "select-none" "pointer-events-none"]}
        "Ollama UI requires an active Ollama server to work")
     ($ :div {:class ["rounded-md" "bg-white/5" "py-3" "px-4" "text-white" "font-mono" "text-sm"]}
        ($ :span {:class ["text-white/25" "mr-2" "select-none"]} "$")
        "ollama serve")))

(defnc Main []

  (use-effect
   :once
   (dispatch [:get-models]))

  (let [ollama-offline? (use-sub [:ollama-offline?])]
    ($ :div {:class ["flex" "flex-col" "w-full" "h-full"]}
       (if ollama-offline?
         ($ Offline)
         (<>
          ($ Header)
          ($ Dialog)
          ($ Footer))))))
