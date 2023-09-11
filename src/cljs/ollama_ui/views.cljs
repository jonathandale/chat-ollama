(ns ollama-ui.views
  (:require [applied-science.js-interop :as j]
            [clojure.string :as str]
            [clojure.set :refer [union]]
            [ollama-ui.lib :refer [defnc debounce]]
            [helix.core :refer [$ <>]]
            [helix.hooks :refer [use-effect use-state use-ref]]
            [refx.alpha :refer [use-sub dispatch]]
            ["lucide-react" :refer [ArrowRight Component Check
                                    Plus MessagesSquare SendHorizontal]]))

(defonce max-textarea-height 100)
(defonce min-textarea-height 50)

(defnc Footer []
  (let [selected-dialog (use-sub [:selected-dialog])
        selected-model (use-sub [:selected-model])
        [prompt set-prompt!] (use-state nil)
        slowly-set-prompt! (debounce set-prompt! 150)
        ref! (use-ref nil)
        send! #(do
                 (dispatch [:send-prompt {:selected-dialog selected-dialog
                                          :prompt prompt}])
                 (j/assoc! @ref! :value ""))]

    (use-effect
     [prompt]
     (when @ref!
       (j/assoc-in! @ref! [:style :height] "auto")
       (j/assoc-in! @ref!
                    [:style :height]
                    (str (min max-textarea-height
                              (max min-textarea-height
                                   (j/get @ref! :scrollHeight))) "px"))))

    ($ :div {:class ["relative"]}
       ($ :textarea {:ref ref!
                     :placeholder (str "Send message to " selected-model)
                     :onChange #(slowly-set-prompt! (j/get-in % [:target :value]))
                     :onKeyPress #(when (and (= (j/get % :key) "Enter")
                                             (j/get % :shiftKey))
                                    (js/console.log "shift+enter")
                                    (j/call % :preventDefault)
                                    (send!))
                     :rows 1
                     :class ["w-full" "resize-none" "rounded-md" "border" "border-transparent"
                             "px-4" "py-3" "bg-gray-800/50" "text-base" "font-normal"
                             "text-white" "outline-none" "focus:bg-gray-800/75"
                             "placeholder:text-white/40"]})
       ($ :button {:class ["absolute" "right-0" "bottom-0" "mr-3" "mb-4"]
                   :on-click send!}
          ($ SendHorizontal)))))

(defnc Message [{:keys [user? children]}]
  ($ :div {:class ["rounded-md" "px-3.5" "py-2.5" "max-w-[75%]"
                   (if user?
                     "bg-white text-gray-900 place-self-end"
                     "bg-gray-700/50 text-white place-self-start flex flex-col gap-2")]}
     children))

(defnc Exchanges []
  (let [ref! (use-ref nil)
        exchanges (use-sub [:dialog-exchanges])]

    (use-effect
     [exchanges]
     (when (some? @ref!)
       (let [scroll-height (j/get @ref! :scrollHeight)]
         (j/assoc! @ref! :scrollTop scroll-height))))

    ($ :div {:ref ref!
             :class ["overflow-scroll" "pb-10"]}
       (for [{:keys [prompt answer] :as exchange} exchanges]
         ($ :div {:class ["flex" "flex-col" "gap-3"]
                  :key (:timestamp exchange)}
            ($ Message {:user? true}
               ($ :p {} prompt))
            ($ Message {:user? false}
               (if (map? answer)
                 (for [[idx text] answer]
                   ($ :p {:class []
                          :key idx} text))
                 ($ :div {:class ["animate-pulse" "gap-2"]}
                    ($ :div {:class ["h-2" "bg-white/20" "rounded"]})
                    ($ :div {:class ["h-2" "bg-white/20" "rounded" "w-[90%]"]})))))))))

(defnc Dialog []
  ($ :div {:class ["flex" "flex-col" "grow" "max-w-5xl" "mx-auto" "justify-end" "px-6" "pb-6"]}
     ($ Exchanges)
     ($ Footer)))

(defnc SidebarItem [{:keys [selected? on-click children]}]
  (let [class #{"bg-gray-800/80" "hover:bg-gray-800"}
        selected-class #{"bg-white" "text-gray-900" "cursor-pointer"}]
    ($ :button {:class (vec (union #{"flex" "justify-between" "items-center" "pl-3" "pr-2"
                                     "py-1.5" "text-sm" "w-full" "text-left" "rounded-sm"}
                                   (if selected? selected-class class)))
                :on-click on-click}
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
                          ($ :span {:class ["opacity-50" "grow"]} ":" model-version)))))))))
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
                         ($ :p {} (:timestamp model-dialog))))))
               ($ :p {:class ["text-white/40"]}
                  (str "No dialogs found for " selected-model)))))
       ($ :button {:class ["flex" "justify-between" "w-full" "rounded-sm" "mt-6" "px-3" "py-2" "bg-sky-600" "text-white"]
                   :on-click #(dispatch [:new-dialog {:model-name selected-model
                                                      :set-selected? true}])}
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
        "OLLAMA_ORIGINS=tauri://localhost:* ollama serve")))

(defnc Main []

  (use-effect
   :once
   (dispatch [:get-models]))

  (let [ollama-offline? (use-sub [:ollama-offline?])]
    ($ :div {:class ["flex" "w-full" "h-full"]}
       (if ollama-offline?
         ($ Offline)
         ($ :div {:class ["flex" "dark:text-white" "relative" "w-full"]}
            ($ Sidebar)
            ($ Dialog))))))
