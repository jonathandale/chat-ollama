(ns ollama-ui.views
  (:require [applied-science.js-interop :as j]
            [clojure.string :as str]
            [clojure.set :refer [union]]
            [ollama-ui.lib :refer [defnc]]
            [cljs-bean.core :refer [->js]]
            [helix.core :refer [$ <>]]
            [helix.hooks :refer [use-effect]]
            [refx.alpha :refer [use-sub dispatch]]
            ["lucide-react" :refer [ArrowRight Component Check Plus MessagesSquare]]))

(defn- b->gb [bytes]
  (.toFixed (/ bytes 1024 1024 1024) 2))

(defnc IconButton [{:keys [icon on-click]}]
  ($ :button {:on-click on-click
              :class ["rounded-sm" "dark:hover:bg-gray-900" "w-12"
                      "h-12" "flex" "items-center" "justify-center"]}
     ($ icon)))

(defnc Footer []
  ($ :div {:class ["max-w-md" "w-full" "mx-auto"]}
     ($ :p "Footer")))

(defnc Dialog []
  ($ :div {:data-tauri-drag-region true
           :class ["grow" "w-full" "max-w-md" "mx-auto"]}
     ($ :p "Dialog")))

(defnc SidebarItem [{:keys [selected? on-click children]}]
  (let [class #{"bg-gray-800/80" "hover:bg-gray-800"}
        selected-class #{"bg-white" "text-gray-900" "cursor-pointer"}]
    ($ :button {:class (vec (union #{"flex" "justify-between" "items-center" "pl-3" "pr-2"
                                     "py-1.5" "text-sm" "w-full" "text-left" "rounded-sm"}
                                   (if selected? selected-class class)))
                :on-click on-click #_#(dispatch [:set-selected-model (:name model)])}
       children
       (if selected?
         ($ Check)
         ($ ArrowRight)))))

(defnc Sidebar []
  (let [selected-model (use-sub [:selected-model])
        selected-dialog (use-sub [:selected-dialog])
        models (use-sub [:models])
        model-dialogs (use-sub [:model-dialogs])]

    ($ :div {:data-tauri-drag-region true
             :class ["dark:bg-gray-950/50" "w-[350px]" "flex" "flex-col" "shrink-0" "p-6"]}
       ($ :div {:class ["mb-8"]}
          ($ :div {:class ["flex" "items-center" "my-3" "gap-3"]}
             ($ Component)
             ($ :p {:class ["text-lg"]}
                "Models"))
          ($ :ul {:class ["flex" "flex-col" "gap-y-2"]}
             (when (seq models)
               (for [model models]
                 (let [[model-name model-version] (str/split (:name model) #":")
                       selected? (= selected-model (:name model))]
                   ($ :li {:key (:digest model)}
                      ($ SidebarItem {:selected? selected?
                                      :on-click #(dispatch [:set-selected-model (:name model)])}
                         (<>
                          model-name
                          ($ :span {:class ["opacity-50 grow"]} ":" model-version)))))))))
       ($ :div {:class ["flex" "items-center" "my-3" "gap-3"]}
          ($ MessagesSquare)
          ($ :p {:class ["text-lg"]}
             "Dialogs"
             ($ :span {:class ["opacity-50" "ml-2"]} (count model-dialogs))))
       ($ :div {:class ["grow" "overflow-scroll"]}
          ($ :ul {:class ["flex" "flex-col" "gap-y-2"]}
             (if (seq model-dialogs)
               (for [model-dialog model-dialogs]
                 (let [selected? (= selected-dialog (:uuid model-dialog))]
                   ($ :li {:key (:uuid model-dialog)}
                      ($ SidebarItem {:selected? selected?
                                      :on-click #(dispatch [:set-selected-dialog (:uuid model-dialog)])}
                         ($ :p {} (:uuid model-dialog))))))
               ($ :p {:class ["text-white/40"]}
                  (str "No dialogs found for " selected-model)))))
       ($ :button {:class ["flex" "justify-between" "w-full" "rounded-sm" "mt-6" "px-3" "py-2" "bg-sky-600" "text-white"]
                   :on-click #(dispatch [:new-dialog selected-model])}
          "Add Dialog"
          ($ Plus)))))

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
    ($ :div {:class ["flex" "w-full" "h-full"]}
       (if ollama-offline?
         ($ Offline)
         ($ :div {:class ["flex" "dark:text-white" "relative"]}
            ($ Sidebar)
            ($ Dialog))))))
