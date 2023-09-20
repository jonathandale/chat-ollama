(ns chat-ollama.views
  (:require [applied-science.js-interop :as j]
            [clojure.string :as str]
            [clojure.set :refer [union]]
            [chat-ollama.lib :refer [defnc]]
            [chat-ollama.utils :refer [debounce local-storage-set! local-storage-get]]
            [chat-ollama.hooks :refer [use-copy-to-clipboard]]
            [helix.core :refer [$ <>]]
            [helix.hooks :refer [use-effect use-state use-ref]]
            [refx.alpha :refer [use-sub dispatch]]
            ["react-markdown$default" :as ReactMarkdown]
            ["react-syntax-highlighter/dist/esm/styles/hljs" :as p :refer [nord githubGist]]
            ["react-syntax-highlighter" :refer [Light]]
            ["react-hotkeys-hook" :refer [useHotkeys]]
            ["date-fns" :refer [formatDistance fromUnixTime parseISO]]
            ["lucide-react" :refer [Clipboard Check Plus X User MessagesSquare Trash2
                                    PanelLeftClose PanelLeftOpen SendHorizontal
                                    ArrowUpToLine ArrowDownToLine]]))
(defonce dark-mode? (j/get (js/matchMedia "(prefers-color-scheme: dark)") :matches))
(defonce max-textarea-height 500)
(defonce min-textarea-height 48)
(defonce line-height 48)
(defonce ls-chat-ollama-prefs "chat-ollama:prefs:")

(defn- b->gb [bytes]
  (j/call (/ bytes 1024 1024 1024) :toFixed 2))

(defnc Ollama []
  ($ :svg {:class ["dark:fill-gray-900" "fill-white" "w-[21px]" "h-[27px]" "scale-110"]
           :xmlns "http://www.w3.org/2000/svg"}
     ($ :path {:d "M19.642 27h-1.498c.315-1.119.308-2.208-.022-3.266-.177-.568-.915-1.363-.497-1.933 1.421-1.94 1.16-4.045.06-5.995-.133-.234-.148-.542.014-.74 1.088-1.333 1.29-2.789.606-4.369-.56-1.293-1.861-2.349-3.327-2.3-.253.007-.495.016-.726.027a.29.29 0 0 1-.28-.177c-.498-1.168-1.373-1.928-2.624-2.281-1.737-.49-3.658.459-4.423 2.072-.116.244-.147.388-.468.377-.422-.015-.859-.056-1.255.025-2.717.554-3.876 3.896-2.47 6.136.333.528.816.613.353 1.378-1.063 1.762-1.203 4.146.12 5.822.453.576-.384 1.567-.547 2.18-.26.983-.24 1.998.058 3.044H1.211c-.417-1.445-.269-3.32.508-4.648a.081.081 0 0 0-.002-.092C.424 20.28.52 17.66 1.567 15.603a.092.092 0 0 0-.006-.096c-1.279-1.93-1.228-4.524.15-6.385.304-.41.775-.836 1.173-1.236a.102.102 0 0 0 .029-.093 9.956 9.956 0 0 1 .172-4.504c.262-.967.991-2.224 2.099-2.177 1.7.072 2.336 2.658 2.426 3.966a.045.045 0 0 0 .066.036c1.822-1.041 3.643-1.037 5.463.012a.07.07 0 0 0 .104-.056c.073-1.126.441-2.537 1.234-3.384.534-.57 1.306-.75 1.97-.378 1.819 1.018 1.803 4.83 1.494 6.509a.09.09 0 0 0 .028.087c.4.374.659.622.777.745 1.713 1.775 1.845 4.76.526 6.818a.088.088 0 0 0-.004.094c1.053 2.066 1.175 4.724-.145 6.715a.1.1 0 0 0 0 .108c.248.374.428.785.54 1.234a6.65 6.65 0 0 1-.02 3.382ZM5.197 2.62a.07.07 0 0 0-.048-.018.066.066 0 0 0-.047.02c-.93.929-.984 3.236-.81 4.435.006.046.031.063.075.052a8.11 8.11 0 0 1 1.576-.222.114.114 0 0 0 .083-.04c.113-.13.17-.23.174-.301.044-1.116-.128-3.116-1.003-3.926Zm10.602.046a.165.165 0 0 0-.25.023c-.76 1.06-.933 2.549-.904 3.815.002.087.058.2.168.34.022.029.05.043.086.044a6.516 6.516 0 0 1 1.6.24.045.045 0 0 0 .051-.018.046.046 0 0 0 .007-.018c.154-1.116.127-3.574-.758-4.426Z"})
     ($ :path {:d "M13.48 13.144c2.105 2.046.448 4.854-2.154 5.035-.502.035-1.099.037-1.789.006-1.834-.08-3.609-1.734-2.989-3.708.894-2.843 4.981-3.23 6.932-1.333Zm-.323 1.199c-.874-1.46-2.958-1.69-4.342-1.008-.75.369-1.446 1.142-1.387 2.025.148 2.264 3.936 2.163 5.141 1.372.85-.56 1.109-1.518.588-2.39ZM4.607 12.684c-.29.5-.154 1.121.301 1.386.455.265 1.059.075 1.348-.426.289-.5.154-1.12-.302-1.386-.455-.265-1.058-.074-1.347.427ZM14.596 13.65c.293.498.898.683 1.351.414.454-.27.583-.89.29-1.388-.293-.497-.898-.682-1.35-.413-.454.269-.584.89-.29 1.387Z"})
     ($ :path {:d "M9.954 15.208c-.297-.103-.445-.31-.444-.622 0-.034.012-.065.033-.09.261-.31.536-.223.812-.034a.085.085 0 0 0 .103-.004c.206-.165.525-.253.728-.033.34.37-.113.64-.37.83a.08.08 0 0 0-.032.073l.06.572a.12.12 0 0 1-.028.091c-.155.195-.359.25-.612.168-.389-.126-.196-.58-.187-.86 0-.046-.02-.077-.063-.091Z"})))

(defnc OllamaAsleep []
  ($ :svg {:class ["fill-gray-900" "dark:fill-white" "w-[77px]" "h-[101px]"]
           :xmlns "http://www.w3.org/2000/svg"}
     ($ :path {:d "M74.203 100.5h-5.787c1.216-4.322 1.189-8.527-.083-12.616-.684-2.193-3.535-5.262-1.921-7.464 5.49-7.497 4.483-15.626.23-23.157-.511-.905-.57-2.096.055-2.862 4.205-5.145 4.985-10.769 2.34-16.873-2.16-4.992-7.187-9.071-12.848-8.886-.979.03-1.914.066-2.806.105a1.12 1.12 0 0 1-1.082-.682c-1.923-4.51-5.301-7.447-10.135-8.81-6.71-1.895-14.127 1.772-17.084 8.002-.448.943-.566 1.499-1.807 1.456-1.631-.058-3.317-.214-4.848.097-10.496 2.139-14.97 15.05-9.54 23.7 1.284 2.042 3.15 2.37 1.363 5.326-4.105 6.802-4.646 16.013.462 22.487 1.752 2.223-1.48 6.05-2.11 8.42-1.006 3.797-.932 7.716.223 11.757H3.013c-1.61-5.582-1.04-12.823 1.962-17.954a.314.314 0 0 0-.008-.354C-.028 74.54.34 64.42 4.384 56.48a.355.355 0 0 0-.021-.375c-4.94-7.45-4.743-17.474.579-24.66 1.174-1.582 2.994-3.228 4.533-4.773a.393.393 0 0 0 .109-.362c-1.107-5.854-.885-11.652.666-17.394C11.261 5.178 14.08.324 18.356.505c6.571.278 9.024 10.267 9.372 15.319a.172.172 0 0 0 .256.139c7.036-4.022 14.07-4.006 21.101.046a.268.268 0 0 0 .403-.215c.28-4.348 1.702-9.8 4.763-13.07 2.063-2.206 5.045-2.897 7.611-1.461 7.024 3.931 6.965 18.657 5.77 25.14a.344.344 0 0 0 .11.336c1.542 1.445 2.543 2.405 3.002 2.88 6.613 6.853 7.124 18.383 2.03 26.335a.34.34 0 0 0-.017.362c4.067 7.981 4.54 18.249-.558 25.935a.385.385 0 0 0 0 .421 15.113 15.113 0 0 1 2.084 4.766c1.099 4.434 1.072 8.788-.08 13.062ZM18.406 6.331a.272.272 0 0 0-.185-.071.256.256 0 0 0-.18.075c-3.589 3.591-3.803 12.503-3.132 17.133.025.177.123.244.294.202a31.325 31.325 0 0 1 6.084-.858.44.44 0 0 0 .323-.156c.436-.5.66-.887.671-1.162.172-4.31-.495-12.035-3.874-15.163Zm40.953.177a.641.641 0 0 0-.965.088c-2.935 4.096-3.606 9.846-3.493 14.738.009.334.225.772.65 1.314a.41.41 0 0 0 .331.168c2.1.053 4.16.363 6.181.93a.176.176 0 0 0 .223-.143c.595-4.31.49-13.803-2.927-17.095Z"})
     ($ :path {:d "M50.402 46.979c8.13 7.906 1.732 18.75-8.32 19.448-1.94.135-4.243.143-6.91.026-7.083-.312-13.94-6.698-11.545-14.326 3.451-10.978 19.24-12.477 26.775-5.148Zm-1.25 4.63c-3.376-5.64-11.423-6.524-16.77-3.893-2.897 1.427-5.585 4.411-5.358 7.825.574 8.744 15.205 8.352 19.86 5.296 3.283-2.16 4.281-5.86 2.268-9.227Z"})
     ($ :path {:d "M36.782 54.952c-1.146-.398-1.718-1.2-1.715-2.404 0-.127.044-.25.126-.345 1.01-1.2 2.071-.863 3.136-.13a.33.33 0 0 0 .398-.017c.797-.636 2.03-.977 2.814-.127 1.313 1.428-.436 2.472-1.43 3.208a.312.312 0 0 0-.121.278l.226 2.21a.465.465 0 0 1-.105.354c-.598.749-1.386.965-2.365.648-1.501-.488-.755-2.24-.721-3.326.003-.176-.078-.293-.243-.349Z"})
     ($ :path {:d "M13.399 43.411a1.726 1.726 0 0 1 2.42-.322l-1.05 1.37 1.05-1.37-.003-.002.001.001.011.009a6.796 6.796 0 0 0 .293.207c.207.142.497.33.826.514.745.419 1.375.642 1.706.642.508 0 1.155-.246 1.79-.622a7.522 7.522 0 0 0 .904-.632l.041-.035.005-.005a1.726 1.726 0 0 1 2.29 2.584l-1.147-1.29 1.146 1.29-.003.002-.003.003-.01.009-.027.023a9.187 9.187 0 0 1-.391.315c-.253.192-.612.448-1.046.705-.822.487-2.116 1.104-3.55 1.104-1.295 0-2.606-.64-3.397-1.084a14.834 14.834 0 0 1-1.492-.965l-.028-.02-.008-.007-.005-.004 1.048-1.371-1.049 1.37a1.726 1.726 0 0 1-.322-2.419Zm42.974-.322.011.008.057.041a11.403 11.403 0 0 0 1.062.68c.744.419 1.374.642 1.706.642.508 0 1.155-.246 1.79-.622a7.522 7.522 0 0 0 .903-.632l.042-.035.002-.002.003-.003a1.726 1.726 0 0 1 2.289 2.584l-1.147-1.29 1.146 1.29-.005.005-.01.009-.027.023a9.187 9.187 0 0 1-.391.315c-.253.192-.612.448-1.046.705-.822.487-2.116 1.104-3.55 1.104-1.295 0-2.607-.64-3.397-1.084a14.834 14.834 0 0 1-1.493-.965l-.027-.02-.009-.007-.003-.003h-.001s-.001-.002 1.047-1.372l-1.048 1.37a1.726 1.726 0 0 1 2.096-2.741Z"})))

(defnc IconButton
  [{:keys [icon on-click disabled? class]
    :or {class []}}]
  ($ :button {:class (into class conj ["disabled:opacity-50" "p-2.5" "rounded"
                                       "dark:enabled:hover:bg-gray-800/40"
                                       "dark:enabled:hover:text-gray-100"
                                       "dark:text-gray-300/80"
                                       "enabled:hover:bg-gray-200/30"
                                       "enabled:hover:text-gray-700"
                                       "text-gray-600/80"])
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
          ($ :div {:class ["z-0" "absolute" "top-0" "-translate-y-full" "inset-x-0" "h-9"
                           "bg-gradient-to-t" "dark:from-gray-900" "from-white" "to-transparent" "pointer-events-none"]})
          ($ :textarea {:ref ref!
                        :key selected-dialog
                        :placeholder (str "Send message to " selected-model)
                        :onChange #(slowly-set-prompt! (j/get-in % [:target :value]))
                        :onKeyPress on-key-press
                        :rows 1
                        :class ["w-full" "resize-none" "rounded" "relative" "z-10" "h-12"
                                "pl-3.5" "pr-10" "py-2.5" "text-base" "font-normal"
                                "dark:bg-gray-950" "border" "placeholder-gray-400/75"
                                "dark:border-gray-200/10" "dark:placeholder-gray-300/40" "border-gray-300/60"
                                "focus:outline-none" "focus:border-cyan-600" "focus:ring-1" "focus:ring-cyan-600"]})
          ($ :button {:class ["absolute" "right-3.5" "bottom-9" "mb-1.5" "z-20" "dark:text-white" "text-gray-700"
                              (when-not (seq prompt) "opacity-20")]
                      :on-click send!}
             ($ SendHorizontal))))))

(defnc Message [{:keys [user? children copy->clipboard]}]
  (let [[copied copy!] (use-copy-to-clipboard)]
    ($ :div {:class ["lg:max-w-[85%]" "flex" "gap-3" "w-full"
                     (if user? "place-self-end flex-row-reverse" "place-self-start")]}
       ($ :div {:class ["shrink-0" "flex" "flex-col"]}
          ($ :div {:class ["rounded" "w-10" "h-10" "flex" "justify-center"
                           (if user?
                             "items-center dark:bg-gray-700/75 bg-gray-200"
                             "items-end dark:bg-white bg-gray-800")
                           (when copy->clipboard "mb-3")]}
             (if user?
               ($ User)
               ($ Ollama)))
          (when copy->clipboard
            ($ IconButton {:icon (if copied Check Clipboard)
                           :on-click #(copy! copy->clipboard)})))
       ($ :div {:class ["h-fit" "w-full" "rounded-md" "p-4" "flex" "flex-col" "gap-2.5" "overflow-scroll"
                        (if user?
                          "border dark:border-gray-100/20 border-gray-400/50"
                          "dark:bg-gray-800/50 bg-gray-50 dark:text-white")]}
          children))))

(defnc Markdown [{:keys [children user?]}]
  ($ ReactMarkdown
     {:children children
      :className "markdown-body"
      :components
      #js{:code
          (fn [props]
            (let [{:keys [inline className children]} (j/lookup props)
                  language (second (str/split className #"-"))
                  [copied copy!] (use-copy-to-clipboard)]
              (if (and (not inline)
                       (seq language))
                (<>
                 ($ :div {:class ["absolute" "right-1.5" "top-1.5" "z-10"]}
                    ($ IconButton {:icon (if copied Check Clipboard)
                                   :on-click #(copy! (first children))}))
                 ($ Light {:children (or (first children) "")
                           :language language
                           :style (if dark-mode? nord githubGist)
                           :customStyle #js {:borderRadius "4px"
                                             :padding "16px"}
                           :className (when user? "border border-gray-200/50")}))
                ($ :code {} children))))}}))

(defnc Dialog []
  (let [ref! (use-ref nil)
        exchanges (use-sub [:dialog-exchanges])
        selected-model (use-sub [:selected-model])
        selected-dialog (use-sub [:selected-dialog])
        {:keys [title model-name]} (use-sub [:dialog selected-dialog])
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
     [title]
     (j/assoc! js/document :title (str model-name " — " (or title "New Chat"))))

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
       ($ :div {:class ["dark:block" "hidden" "z-20" "absolute" "top-0" "inset-x-0" "h-9"
                        "bg-gradient-to-t" "dark:to-gray-900" "to-white" "from-transparent" "pointer-events-none"]})
       ($ :div {:class ["absolute" "top-4" "right-4" "z-30" "flex" "flex-col"]}
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
          ($ :p {:class ["text-sm" "dark:text-gray-100" "text-gray-600" "text-center" "p-6"]}
             model-name
             ($ :span {:class ["opacity-50"]} ":" model-version))
          ($ :div {:class ["flex" "flex-col" "w-full" "grow" "max-w-6xl" "mx-auto" "justify-end" "pt-6" "pb-36" "px-20"]}
             (for [{:keys [prompt answer timestamp meta]} exchanges]
               ($ :div {:class ["flex" "flex-col" "gap-6" "mt-6"]
                        :key timestamp}
                  ($ Message {:user? true}
                     ($ Markdown {:user? true} prompt))
                  ($ Message {:user? false
                              :copy->clipboard (:response meta)}
                     (if answer
                       ($ Markdown {} answer)
                       ($ :div {:class ["flex" "flex-col" "gap-2" "my-1" "animate-pulse" "min-w-[250px]"]}
                          ($ :div {:class ["h-2" "dark:bg-white/10" "bg-gray-200/75" "rounded"]})
                          ($ :div {:class ["h-2" "dark:bg-white/10" "bg-gray-200/75" "rounded" "w-[75%]"]}))))))))
       ($ Footer))))

(defnc SidebarItem [{:keys [selected? on-click children]}]
  (let [class #{"border-transparent"}
        selected-class #{"dark:text-white" "cursor-default" "bg-gray-300/20" "dark:bg-gray-800/50" "border-cyan-600"}]
    ($ :button {:class (vec (union #{"px-3" "py-1.5" "text-sm" "w-full" "text-left" "rounded"
                                     "dark:hover:bg-gray-800/60" "hover:bg-gray-300/20" "border-l-4"}
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
       ($ :h1 {:class ["dark:text-white" "text-3xl"]}
          "Start a new Dialog")
       ($ :h2 {:class ["text-lg" "dark:text-white/40" "text-gray-800/60" "mb-10"]}
          "Choose which model you want to send messages to")
       ($ :ul {:class ["dark:text-white" "dark:bg-gray-950/30" "bg-white/75" "w-full" "max-w-2xl" "overflow-scroll"
                       "rounded" "border" "border-gray-700/40" "divide-y" "divide-gray-700/50"]}
          (for [model models]
            (let [[model-name model-version] (str/split (:name model) #":")]
              ($ :li {:key (:digest model)
                      :class ["hover:text-white"]}
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
                    ($ :div {:class ["p-1.5" "rounded" "bg-cyan-700" "text-white"
                                     "group-hover:bg-white" "group-hover:text-cyan-700"]}
                       ($ Plus))))))))))

(defnc Sidebar [{:keys [set-dialog-chooser! toggle-sidebar!]}]
  (let [selected-model (use-sub [:selected-model])
        selected-dialog (use-sub [:selected-dialog])
        dialogs (use-sub [:dialogs])]

    (<>
     ($ :div {:class ["dark:bg-gray-950" "bg-gray-50" "w-[350px]"
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
           ($ :ul {:class ["flex" "flex-col" "gap-y-1"]}
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
                             ($ :p {:class ["text-xs" "dark:text-gray-100" "text-gray-600/80"]}
                                model-name
                                ($ :span {:class ["opacity-60" "grow"]} ":" model-version))
                             ($ :p {:class ["text-xs" "dark:text-gray-300/50" "text-gray-400"]}
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
        command (if (= "localhost" (j/get js/location :hostname))
                  "ollama serve"
                  (str "OLLAMA_ORIGINS=" (j/get js/location :origin) " ollama serve"))]
    ($ :div {:class ["flex" "flex-col" "grow" "w-full" "justify-center" "items-center"]}
       ($ OllamaAsleep)
       ($ :h1 {:class ["dark:text-white" "text-3xl" "mt-6"]}
          "Looks like Ollama is asleep!")
       ($ :h2 {:class ["text-lg" "dark:text-white/40" "text-gray-800/60" "mb-10"]}
          "Ollama UI requires an active Ollama server to work")
       ($ :div {:class ["flex" "items-center" "rounded-md" "bg-gray-100" "dark:bg-white/5" "py-2" "pr-2" "pl-4"
                        "dark:text-white" "font-mono" "text-sm"]}
          ($ :span {:class ["dark:text-white" "opacity-25" "mr-3" "select-none"]} "$")
          command
          ($ :button {:class ["ml-6" "p-2" "rounded-sm" "dark:hover:bg-gray-900" "hover:bg-gray-200"]
                      :on-click #(copy! command)}
             (if copied
               ($ Check {:size 16})
               ($ Clipboard {:size 16})))))))

(defnc Dialogs []
  (let [[dialog-chooser? set-dialog-chooser!] (use-state false)
        ls-sidebar? (local-storage-get (str ls-chat-ollama-prefs "sidebar?"))
        [show-sidebar? set-show-sidebar!]
        (use-state (if (some? ls-sidebar?)
                     ls-sidebar?
                     true))
        toggle-sidebar! #(set-show-sidebar! not)
        dialogs (use-sub [:dialogs])]

    (useHotkeys "ctrl+n" #(when-not dialog-chooser? (set-dialog-chooser! true)))
    (useHotkeys "ctrl+d" toggle-sidebar!)
    (useHotkeys "esc" #(when dialog-chooser? (set-dialog-chooser! false)))

    (use-effect
     [show-sidebar?]
     (local-storage-set!
      (str ls-chat-ollama-prefs "sidebar?")
      show-sidebar?))

    ($ :div {:class ["flex" "dark:text-white" "relative" "w-full"]}
       (when (or (empty? dialogs) dialog-chooser?)
         ($ :div {:class ["absolute" "inset-0" "dark:bg-gray-900/75" "bg-white/30" "backdrop-blur-md" "z-50" "w-full" "h-full"]}
            ($ StartDialog {:on-close (when (seq dialogs) #(set-dialog-chooser! false))})))
       (if show-sidebar?
         ($ Sidebar {:set-dialog-chooser! set-dialog-chooser!
                     :toggle-sidebar! toggle-sidebar!})
         ($ :div {:class ["absolute" "top-4" "left-4" "z-30"]}
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
