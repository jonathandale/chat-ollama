(ns ollama-ui.views
  (:require [applied-science.js-interop :as j]
            [clojure.string :as str]
            [clojure.set :refer [union]]
            [ollama-ui.lib :refer [defnc]]
            [ollama-ui.utils :refer [debounce local-storage-set! local-storage-get]]
            [ollama-ui.hooks :refer [use-copy-to-clipboard]]
            [helix.core :refer [$ <>]]
            [helix.hooks :refer [use-effect use-state use-ref]]
            [refx.alpha :refer [use-sub dispatch]]
            ["react-markdown$default" :as ReactMarkdown]
            ["react-syntax-highlighter/dist/esm/styles/prism" :refer [nord]]
            ["react-syntax-highlighter" :refer [Prism]]
            ["react-hotkeys-hook" :refer [useHotkeys]]
            ["date-fns" :refer [formatDistance fromUnixTime parseISO]]
            ["lucide-react" :refer [Clipboard Check Plus X User MessagesSquare Trash2
                                    PanelLeftClose PanelLeftOpen SendHorizontal
                                    ArrowUpToLine ArrowDownToLine]]))

(defonce max-textarea-height 500)
(defonce min-textarea-height 48)
(defonce line-height 48)
(defonce ls-ollama-ui-prefs "ollama-ui:prefs:")

(defn- b->gb [bytes]
  (j/call (/ bytes 1024 1024 1024) :toFixed 2))

(defnc Ollama []
  ($ :svg {:class ["dark:fill-gray-900" "fill-white" "w-[21px]" "h-[27px]" "scale-110"]
           :xmlns "http://www.w3.org/2000/svg"}
     ($ :path {:d "M19.642 27h-1.498c.315-1.119.308-2.208-.022-3.266-.177-.568-.915-1.363-.497-1.933 1.421-1.94 1.16-4.045.06-5.995-.133-.234-.148-.542.014-.74 1.088-1.333 1.29-2.789.606-4.369-.56-1.293-1.861-2.349-3.327-2.3-.253.007-.495.016-.726.027a.29.29 0 0 1-.28-.177c-.498-1.168-1.373-1.928-2.624-2.281-1.737-.49-3.658.459-4.423 2.072-.116.244-.147.388-.468.377-.422-.015-.859-.056-1.255.025-2.717.554-3.876 3.896-2.47 6.136.333.528.816.613.353 1.378-1.063 1.762-1.203 4.146.12 5.822.453.576-.384 1.567-.547 2.18-.26.983-.24 1.998.058 3.044H1.211c-.417-1.445-.269-3.32.508-4.648a.081.081 0 0 0-.002-.092C.424 20.28.52 17.66 1.567 15.603a.092.092 0 0 0-.006-.096c-1.279-1.93-1.228-4.524.15-6.385.304-.41.775-.836 1.173-1.236a.102.102 0 0 0 .029-.093 9.956 9.956 0 0 1 .172-4.504c.262-.967.991-2.224 2.099-2.177 1.7.072 2.336 2.658 2.426 3.966a.045.045 0 0 0 .066.036c1.822-1.041 3.643-1.037 5.463.012a.07.07 0 0 0 .104-.056c.073-1.126.441-2.537 1.234-3.384.534-.57 1.306-.75 1.97-.378 1.819 1.018 1.803 4.83 1.494 6.509a.09.09 0 0 0 .028.087c.4.374.659.622.777.745 1.713 1.775 1.845 4.76.526 6.818a.088.088 0 0 0-.004.094c1.053 2.066 1.175 4.724-.145 6.715a.1.1 0 0 0 0 .108c.248.374.428.785.54 1.234a6.65 6.65 0 0 1-.02 3.382ZM5.197 2.62a.07.07 0 0 0-.048-.018.066.066 0 0 0-.047.02c-.93.929-.984 3.236-.81 4.435.006.046.031.063.075.052a8.11 8.11 0 0 1 1.576-.222.114.114 0 0 0 .083-.04c.113-.13.17-.23.174-.301.044-1.116-.128-3.116-1.003-3.926Zm10.602.046a.165.165 0 0 0-.25.023c-.76 1.06-.933 2.549-.904 3.815.002.087.058.2.168.34.022.029.05.043.086.044a6.516 6.516 0 0 1 1.6.24.045.045 0 0 0 .051-.018.046.046 0 0 0 .007-.018c.154-1.116.127-3.574-.758-4.426Z"})
     ($ :path {:d "M13.48 13.144c2.105 2.046.448 4.854-2.154 5.035-.502.035-1.099.037-1.789.006-1.834-.08-3.609-1.734-2.989-3.708.894-2.843 4.981-3.23 6.932-1.333Zm-.323 1.199c-.874-1.46-2.958-1.69-4.342-1.008-.75.369-1.446 1.142-1.387 2.025.148 2.264 3.936 2.163 5.141 1.372.85-.56 1.109-1.518.588-2.39ZM4.607 12.684c-.29.5-.154 1.121.301 1.386.455.265 1.059.075 1.348-.426.289-.5.154-1.12-.302-1.386-.455-.265-1.058-.074-1.347.427ZM14.596 13.65c.293.498.898.683 1.351.414.454-.27.583-.89.29-1.388-.293-.497-.898-.682-1.35-.413-.454.269-.584.89-.29 1.387Z"})
     ($ :path {:d "M9.954 15.208c-.297-.103-.445-.31-.444-.622 0-.034.012-.065.033-.09.261-.31.536-.223.812-.034a.085.085 0 0 0 .103-.004c.206-.165.525-.253.728-.033.34.37-.113.64-.37.83a.08.08 0 0 0-.032.073l.06.572a.12.12 0 0 1-.028.091c-.155.195-.359.25-.612.168-.389-.126-.196-.58-.187-.86 0-.046-.02-.077-.063-.091Z"})))

(defnc IconButton
  [{:keys [icon on-click disabled? class]
    :or {class []}}]
  ($ :button {:class (into class conj ["enabled:hover:bg-gray-800/40" "p-2.5" "rounded"
                                       "enabled:hover:text-gray-100" "text-gray-300/80"
                                       "disabled:opacity-50"])
              :on-click on-click
              :disabled disabled?}
     ($ icon {:size 20})))

(defnc Footer []
  (let [selected-dialog (use-sub [:selected-dialog])
        selected-model (use-sub [:selected-model])
        [prompt set-prompt!] (use-state nil)
        slowly-set-prompt! (debounce set-prompt! 150)
        ref! (use-ref nil)
        send! #(do
                 (dispatch [:send-prompt {:selected-dialog selected-dialog
                                          :prompt prompt}])
                 (j/assoc! @ref! :value "")
                 (set-prompt! nil))
        on-key-press #(when (and (= (j/get % :key) "Enter")
                                 (not (j/get % :shiftKey)))
                        (j/call % :preventDefault)
                        (send!))]

    (use-effect
     [prompt]
     (when @ref!
       (j/assoc-in! @ref! [:style :height] "auto")
       (j/assoc-in! @ref!
                    [:style :height]
                    (str (min max-textarea-height
                              (max min-textarea-height
                                   (j/get @ref! :scrollHeight))) "px"))))

    (use-effect
     [@ref!]
     (when @ref!
       (j/call @ref! :focus)))

    ($ :div {:class ["absolute" "bottom-0" "inset-x-0"]}
       ($ :div {:class ["dark:bg-gray-900" "bg-white" "z-10" "max-w-5xl" "mx-auto" "absolute" "bottom-0" "pb-6" "inset-x-16"]}
          ($ :div {:class ["z-0" "absolute" "top-0" "-translate-y-full" "inset-x-0" "h-16"
                           "bg-gradient-to-t" "dark:from-gray-900" "from-white" "to-transparent" "pointer-events-none"]})
          ($ :textarea {:ref ref!
                        :key selected-dialog
                        :placeholder (str "Send message to " selected-model)
                        :onChange #(slowly-set-prompt! (j/get-in % [:target :value]))
                        :onKeyPress on-key-press
                        :rows 1
                        :class ["w-full" "resize-none" "rounded" "relative" "z-10" "h-12"
                                "pl-3.5" "pr-10" "py-2.5" "text-base" "font-normal"
                                "dark:bg-gray-950" "border"
                                "border-gray-200/10" "placeholder-gray-300/40"
                                "focus:outline-none" "focus:border-cyan-600" "focus:ring-1" "focus:ring-cyan-600"]})
          ($ :button {:class ["absolute" "right-3.5" "bottom-9" "mb-1.5" "z-20" "dark:text-white" "text-gray-700"
                              (when-not (seq prompt) "opacity-20")]
                      :on-click send!}
             ($ SendHorizontal))))))

(defnc Message [{:keys [user? children copy->clipboard]}]
  (let [[copied copy!] (use-copy-to-clipboard)]
    ($ :div {:class ["lg:max-w-[85%]" "flex" "gap-3"
                     (if user? "place-self-end flex-row-reverse" "place-self-start")]}
       ($ :div {:class ["shrink-0" "flex" "flex-col"]}
          ($ :div {:class ["rounded" "w-10" "h-10" "flex" "justify-center"
                           (if user?
                             "items-center dark:bg-gray-700/75 bg-gray-200"
                             "items-end dark:bg-white bg-gray-800")
                           (when copy->clipboard "mb-4")]}
             (if user?
               ($ User)
               ($ Ollama)))
          (when copy->clipboard
            ($ IconButton {:icon (if copied Check Clipboard)
                           :on-click #(copy! copy->clipboard)})))
       ($ :div {:class ["h-fit" "rounded-md" "px-4" "py-3" "flex" "flex-col" "gap-2.5" "overflow-scroll"
                        (if user?
                          "dark:bg-white dark:text-gray-900 bg-gray-800 text-white"
                          "dark:bg-gray-800/50 bg-gray-50 dark:text-white")]}
          children))))

(defnc Dialog []
  (let [ref! (use-ref nil)
        exchanges (use-sub [:dialog-exchanges])
        selected-model (use-sub [:selected-model])
        selected-dialog (use-sub [:selected-dialog])
        [model-name model-version] (str/split selected-model #":")
        [->top-disabled? set->top-disabled] (use-state true)
        [->bottom-disabled? set->bottom-disabled] (use-state true)
        ->top #(j/call @ref!
                       :scrollTo
                       #js{:top 0 :behavior "smooth"})
        ->bottom #(j/call @ref!
                          :scrollTo
                          #js{:top (j/get @ref! :scrollHeight)
                              :behavior "smooth"})
        get-scroll-info #(let [height (-> @ref!
                                          (j/call :getBoundingClientRect)
                                          (j/get :height))
                               scroll-height (j/get @ref! :scrollHeight)
                               scroll-top (j/get @ref! :scrollTop)]
                           {:scroll-top scroll-top
                            :scroll-bottom (- scroll-height (+ height scroll-top))})
        slow-on-scroll
        (debounce (fn []
                    (let [{:keys [scroll-bottom scroll-top]}
                          (get-scroll-info)]
                      (set->top-disabled (not (pos? scroll-top)))
                      (set->bottom-disabled (not (pos? scroll-bottom)))))
                  500)]

    (use-effect
     [exchanges]
     (when (some? @ref!)
       (let [{:keys [scroll-bottom]} (get-scroll-info)]
         (when (<= scroll-bottom line-height)
           (j/assoc! @ref!
                     :scrollTop
                     (j/get @ref! :scrollHeight))))))
    (use-effect
     [(count exchanges)]
     (when (some? @ref!)
       (j/assoc! @ref!
                 :scrollTop
                 (j/get @ref! :scrollHeight))))

    (useHotkeys "ctrl+shift+up" ->top)
    (useHotkeys "ctrl+shift+down" ->bottom)

    ($ :div {:class ["flex" "flex-col" "relative" "w-full" "h-screen"]}
       ($ :div {:class ["absolute" "top-4" "right-4" "z-20" "flex" "flex-col"]}
          ($ IconButton {:on-click #(dispatch [:delete-dialog selected-dialog])
                         :icon Trash2})
          ($ IconButton {:on-click ->top
                         :disabled? ->top-disabled?
                         :icon ArrowUpToLine})
          ($ IconButton {:on-click ->bottom
                         :disabled? ->bottom-disabled?
                         :icon ArrowDownToLine}))
       ($ :div {:ref ref!
                :class ["relative" "grow" "flex" "flex-col" "w-full" "overflow-scroll"]
                :on-scroll slow-on-scroll}
          ($ :p {:class ["text-sm" "dark:text-gray-100" "text-gray-500" "text-center" "p-6"]}
             model-name
             ($ :span {:class ["opacity-50"]} ":" model-version))
          ($ :div {:class ["flex" "flex-col" "w-full" "grow" "max-w-5xl" "mx-auto" "justify-end" "pt-6" "pb-32" "px-[4.5rem]"]}
             (for [{:keys [prompt answer timestamp meta]} exchanges]
               ($ :div {:class ["flex" "flex-col" "gap-6" "mt-6"]
                        :key timestamp}
                  (let [prompt? (seq (str/trim prompt))]
                    ($ Message {:user? true}
                       ($ :p {:class ["whitespace-pre-line" (when-not prompt? "text-gray-400 italic")]}
                          (if prompt? prompt "Empty"))))
                  ($ Message {:user? false
                              :copy->clipboard (:response meta)}
                     (if answer
                       ($ ReactMarkdown
                          {:children answer
                           :className "markdown-body"
                           :components
                           #js{:code
                               (fn [props]
                                 (let [{:keys [inline className children]} (j/lookup props)
                                       language (second (str/split className #"-"))]
                                   (if (and (not inline)
                                            (seq language))
                                     ($ Prism {:children (or (first children) "")
                                               :language language
                                               :style nord})
                                     ($ :code {} children))))}})
                       ($ :div {:class ["flex" "flex-col" "gap-2" "my-1" "animate-pulse" "min-w-[250px]"]}
                          ($ :div {:class ["h-2" "dark:bg-white/10" "bg-gray-200/75" "rounded"]})
                          ($ :div {:class ["h-2" "dark:bg-white/10" "bg-gray-200/75" "rounded" "w-[75%]"]}))))))))
       ($ Footer))))

(defnc SidebarItem [{:keys [selected? on-click children]}]
  (let [class #{"border-transparent"}
        selected-class #{"dark:text-white" "cursor-default" "bg-gray-100/60" "dark:bg-gray-800/50" "border-cyan-500"}]
    ($ :button {:class (vec (union #{"px-3" "py-1.5" "text-sm" "w-full" "text-left" "rounded"
                                     "dark:hover:bg-gray-800/60" "hover:bg-gray-100/50" "border-l-4"}
                                   (if selected? selected-class class)))
                :on-click on-click}
       children)))

(defnc StartDialog [{:keys [on-close]}]
  (let [models (use-sub [:models])]
    ($ :div {:class ["flex" "flex-col" "grow" "w-full"
                     "justify-center" "items-center"
                     "py-20" "h-full"]}
       (when (fn? on-close)
         ($ :div {:class ["absolute" "top-4" "right-4"]}
            ($ IconButton {:on-click on-close
                           :icon X})))
       ($ :h1 {:class ["text-white" "text-3xl"]}
          "Start a new Dialog")
       ($ :h2 {:class ["text-lg" "text-white/40" "mb-10"]}
          "Choose which model you want to send messages to")
       ($ :ul {:class ["text-white" "bg-gray-950/30" "w-full" "max-w-2xl" "overflow-scroll"
                       "rounded" "border" "border-gray-700/40" "divide-y" "divide-gray-700/50"]}
          (for [model models]
            (let [[model-name model-version] (str/split (:name model) #":")]
              ($ :li {:key (:digest model)}
                 ($ :button {:class ["text-left" "w-full" "hover:bg-cyan-700" "pl-3" "pr-5" "py-2"
                                     "flex" "items-center" "justify-between" "group"]
                             :on-click #(do
                                          (dispatch [:new-dialog (:name model)])
                                          (when (fn? on-close)
                                            (on-close)))}
                    ($ :div
                       ($ :p {:class ["text-lg"]}
                          model-name
                          ($ :span {:class ["opacity-50"]} ":" model-version))
                       ($ :p {:class ["flex" "text-sm" "gap-3"]}
                          ($ :span {:class ["opacity-60"]}
                             (formatDistance
                              (parseISO (:modified_at model))
                              (new js/Date)
                              #js {:addSuffix true}))
                          ($ :span {:class ["opacity-40"]}
                             (b->gb (:size model)) "GB")
                          ($ :span {:class ["opacity-20"]}
                             (subs (:digest model) 0 7))))
                    ($ :div {:class ["p-1.5" "rounded" "bg-cyan-700" "group-hover:bg-white" "group-hover:text-cyan-700"]}
                       ($ Plus))))))))))

(defnc Sidebar [{:keys [set-dialog-chooser! toggle-sidebar!]}]
  (let [selected-model (use-sub [:selected-model])
        selected-dialog (use-sub [:selected-dialog])
        dialogs (use-sub [:dialogs])]

    (<>
     ($ :div {:class ["dark:bg-gray-950" "bg-gray-50/50" "w-[350px]"
                      "flex" "flex-col" "shrink-0" "p-6"]}
        ($ :div {:class ["flex" "items-center" "justify-between" "mb-4"]}
           ($ :div {:class ["flex" "items-center" "gap-3"]}
              ($ MessagesSquare)
              ($ :p {:class ["text-lg"]}
                 "Dialogs"
                 ($ :span {:class ["opacity-50" "ml-2"]}
                    (count dialogs))))
           ($ IconButton {:class ["-m-2"]
                          :on-click toggle-sidebar!
                          :icon PanelLeftClose}))
        ($ :div {:class ["grow" "overflow-scroll"]}
           ($ :ul {:class ["flex" "flex-col" "gap-y-2"]}
              (if (seq dialogs)
                (for [[uuid dialog] dialogs]
                  (let [selected? (= selected-dialog uuid)
                        [model-name model-version] (str/split (:model-name dialog) #":")]
                    ($ :li {:key uuid}
                       ($ SidebarItem {:selected? selected?
                                       :on-click #(do
                                                    (dispatch [:set-selected-dialog uuid])
                                                    (dispatch [:set-selected-model (:model-name dialog)]))}
                          ($ :p {:class ["truncate" (when-not (:title dialog) "italic opacity-75")]} (or (:title dialog) "New Chat"))
                          ($ :div {:class ["flex" "items-center" "justify-between"]}
                             ($ :p {:class ["text-xs" "dark:text-gray-100" "text-gray-500"]}
                                model-name
                                ($ :span {:class ["opacity-50" "grow"]} ":" model-version))
                             ($ :p {:class ["text-xs" "dark:text-gray-300/50" "text-gray-300"]}
                                (formatDistance
                                 (fromUnixTime (:timestamp dialog))
                                 (new js/Date)
                                 #js {:addSuffix true})))))))
                ($ :p {:class ["dark:text-white/40"]}
                   (str "No dialogs found for " selected-model)))))
        ($ :button {:on-click #(set-dialog-chooser! true)
                    :class ["bg-cyan-600" "hover:bg-cyan-700" "text-white" "flex" "px-4" "py-2.5"
                            "items-center" "rounded" "justify-between"]}
           "New dialog"
           ($ Plus))))))

(defnc Offline []
  (let [[copied copy!] (use-copy-to-clipboard)
        command (str "OLLAMA_ORIGINS=" (j/get js/location :origin) " ollama serve")]
    ($ :div {:class ["flex" "flex-col" "grow" "w-full" "justify-center" "items-center"]}
       ($ :img {:class ["w-20" "h-auto" "mb-10"]
                :src "./assets/ollama-asleep.svg"
                :alt "Looks like Ollama is Offline"})
       ($ :h1 {:class ["text-white" "text-3xl"]}
          "Looks like Ollama is asleep!")
       ($ :h2 {:class ["text-lg" "text-white/40" "mb-10"]}
          "Ollama UI requires an active Ollama server to work")
       ($ :div {:class ["flex" "items-center" "rounded-md" "bg-white/5" "py-2" "pr-2" "pl-4" "text-white" "font-mono" "text-sm"]}
          ($ :span {:class ["text-white/25" "mr-3" "select-none"]} "$")
          command
          ($ :button {:class ["ml-6" "p-2" "rounded-sm" "hover:bg-gray-900"]
                      :on-click #(copy! command)}
             (if copied
               ($ Check {:size 16})
               ($ Clipboard {:size 16})))))))

(defnc Dialogs []
  (let [[dialog-chooser? set-dialog-chooser!] (use-state false)
        [show-sidebar? set-show-sidebar!]
        (use-state (local-storage-get
                    (str ls-ollama-ui-prefs "sidebar?")))
        toggle-sidebar! #(set-show-sidebar! not)
        dialogs (use-sub [:dialogs])]

    (useHotkeys "ctrl+n" #(when-not dialog-chooser? (set-dialog-chooser! true)))
    (useHotkeys "ctrl+d" toggle-sidebar!)
    (useHotkeys "esc" #(when dialog-chooser? (set-dialog-chooser! false)))

    (use-effect
     [show-sidebar?]
     (local-storage-set!
      (str ls-ollama-ui-prefs "sidebar?")
      show-sidebar?))

    ($ :div {:class ["flex" "dark:text-white" "relative" "w-full"]}
       (when (or (empty? dialogs) dialog-chooser?)
         ($ :div {:class ["absolute" "inset-0" "bg-gray-900/75" "backdrop-blur-md" "z-50" "w-full" "h-full"]}
            ($ StartDialog {:on-close (when (seq dialogs) #(set-dialog-chooser! false))})))
       (if show-sidebar?
         ($ Sidebar {:set-dialog-chooser! set-dialog-chooser!
                     :toggle-sidebar! toggle-sidebar!})
         ($ :div {:class ["absolute" "top-4" "left-4" "z-10"]}
            ($ IconButton {:on-click toggle-sidebar!
                           :icon PanelLeftOpen})))
       ($ Dialog))))

(defnc Main []
  (let [ollama-offline? (use-sub [:ollama-offline?])]

    (use-effect
     :once
     (dispatch [:get-models]))

    ($ :div {:class ["flex" "w-full" "h-full" "relative"]}
       (cond
         ollama-offline?
         ($ Offline)

         :else
         ($ Dialogs)))))
