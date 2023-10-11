(ns chat-ollama.events
  (:require [cljs.spec.alpha :as s]
            [chat-ollama.db :refer [default-db]]
            [refx.alpha :refer [->interceptor reg-event-db reg-event-fx]]
            [refx.interceptors :refer [after]]
            ["date-fns" :refer (getUnixTime)]))

(defonce api-base "http://127.0.0.1:11434")
(defonce wait-multiplier 1.25)
(defonce wait-max (* 1000 60 5))

(defn check-and-throw
  "Throws an exception if `db` doesn't match the Spec `a-spec`."
  [a-spec db]
  (when-not (s/valid? a-spec db)
    (throw (ex-info (str "spec check failed: " (s/explain-str a-spec db)) {}))))

(def check-spec-interceptor (after (partial check-and-throw :chat-ollama.db/db)))

(def offline-interceptor
  (->interceptor
   :id :offline?
   :after  (fn [{:keys [coeffects] :as context}]
             (let [{:keys [event db]} coeffects
                   offline? (:ollama-offline? db)
                   [_ wait {:keys [url status]}] event]
               (if (and (some? url)
                        (zero? status))
                 (let [new-wait (when (number? wait)
                                  (* wait wait-multiplier))]
                   (cond-> context
                     true
                     (assoc-in [:coeffects :db :ollama-offline?] true)

                     (and (not (false? offline?))
                          (number? new-wait)
                          (< new-wait wait-max))
                     (update-in [:effects :fx]
                                conj
                                [:dispatch-later {:ms new-wait
                                                  :dispatch [:get-models new-wait]}])))
                 context)))))

(def ollama-interceptors
  [offline-interceptor
   check-spec-interceptor])

(reg-event-fx
 :initialise-db
 ollama-interceptors
 (fn [_ _]
   {:db default-db
    :dispatch [:new-dialog]}))

(reg-event-db
 :set-selected-model
 ollama-interceptors
 (fn [db [_ model-name]]
   (assoc db :selected-model model-name)))

;; GET MODELS
(reg-event-fx
 :get-models-success
 ollama-interceptors
 (fn [{:keys [db]} [_ {:keys [models]}]]
   {:db (assoc db
               :models models
               :ollama-offline? false)}))

(reg-event-db
 :get-models-failure
 ollama-interceptors
 (fn [db [_ _wait {:keys [status]}]]
   (assoc db :ollama-offline? (zero? status))))

(reg-event-fx
 :get-models
 ollama-interceptors
 (fn [_ [_ wait]]
   {:fetch {:url (str api-base "/api/tags")
            :method :get
            :on-success [:get-models-success]
            :on-failure [:get-models-failure wait]}}))

(reg-event-db
 :warm-model-success
 identity)

(reg-event-db
 :warm-model-failure
 ollama-interceptors
 (fn [db [_ _wait {:keys [status]}]]
   (assoc db :ollama-offline? (zero? status))))

(reg-event-fx
 :warm-model
 ollama-interceptors
 (fn [_ [_ model-name]]
   (if model-name
     {:fetch {:url (str api-base "/api/generate")
              :method :post
              :body {:model model-name}
              :on-success [:warm-model-success]
              :on-failure [:warm-model-failure]}}
     {})))

;; DIALOGS

(reg-event-fx
 :set-selected-dialog
 ollama-interceptors
 (fn [{:keys [db]} [_ dialog-uuid]]
   (let [selected-dialog (get-in db [:dialogs dialog-uuid])]
     {:db (assoc db :selected-dialog dialog-uuid)
      :dispatch [:warm-model (:model-name selected-dialog)]})))

(reg-event-db
 :set-dialog-model
 ollama-interceptors
 (fn [db [_ dialog-uuid model-name]]
   (-> db
       (assoc-in [:dialogs dialog-uuid :model-name] model-name)
       (assoc :selected-model model-name))))

(reg-event-fx
 :new-dialog
 ollama-interceptors
 (fn [{:keys [db]} [_ model-name]]
   (let [new-uuid (str (random-uuid))
         timestamp (getUnixTime (new js/Date))]
     {:db (-> db
              (assoc-in [:dialogs new-uuid]
                        {:uuid new-uuid
                         :generating? false
                         :model-name model-name
                         :timestamp timestamp})
              (assoc :selected-model model-name
                     :selected-dialog new-uuid))
      :dispatch [:warm-model model-name]})))

(reg-event-fx
 :delete-dialog
 ollama-interceptors
 (fn [{:keys [db]} [_ dialog-uuid]]
   (let [purged (update db :dialogs dissoc dialog-uuid)
         next-selected (->> purged
                            :dialogs
                            vals
                            (sort-by :timestamp)
                            first)
         model-name (:model-name next-selected)]
     (cond-> {:db (-> purged
                      (assoc :selected-model model-name
                             :selected-dialog (:uuid next-selected)))}
       (seq? model-name)
       (assoc :dispatch [:warm-model model-name])))))

;; PROMPTS
(reg-event-fx
 :send-prompt
 ollama-interceptors
 (fn [{:keys [db]} [_ {:keys [selected-dialog prompt set-abort!]}]]
   (let [new-uuid (str (random-uuid))
         timestamp (getUnixTime (new js/Date))
         context (->> (get-in db [:dialogs selected-dialog :exchanges])
                      vals
                      (sort-by > :timestamp)
                      first
                      :meta
                      :context)]
     {:db (cond-> db
            :always
            (assoc-in [:dialogs selected-dialog :generating?] true)
            :always
            (assoc-in [:dialogs selected-dialog :exchanges new-uuid]
                      {:prompt prompt
                       :timestamp timestamp})
            (nil? context)
            (assoc-in [:dialogs selected-dialog :title] prompt))
      :dispatch [:get-answer {:prompt prompt
                              :context context
                              :set-abort! set-abort!
                              :dialog-uuid selected-dialog
                              :exchange-uuid new-uuid}]})))

(reg-event-fx
 :get-answer-success
 ollama-interceptors
 (fn [{:keys [db]} [_ {:keys [dialog-uuid exchange-uuid]} response]]
   {:db (-> db
            (assoc-in [:dialogs dialog-uuid :exchanges exchange-uuid :meta] response)
            (assoc-in [:dialogs dialog-uuid :generating?] false))}))

(reg-event-fx
 :get-answer-progress
 ollama-interceptors
 (fn [{:keys [db]}
      [_
       {:keys [dialog-uuid exchange-uuid]}
       {:keys [text _idx _new-line?]}]]
   {:db (update-in db [:dialogs dialog-uuid :exchanges exchange-uuid :answer]
                   #(str % text))}))

(reg-event-db
 :get-answer-failure
 ollama-interceptors
 (fn [db [_ {:keys [dialog-uuid exchange-uuid]} {:keys [status]}]]
   (-> db
       (assoc :ollama-offline? (zero? status))
       (assoc-in [:dialogs dialog-uuid :generating?] false)
       (assoc-in [:dialogs dialog-uuid :exchanges exchange-uuid :failed?] true))))

(reg-event-db
 :get-answer-abort
 ollama-interceptors
 (fn [db [_ {:keys [dialog-uuid exchange-uuid]} _]]
   (-> db
       (assoc-in [:dialogs dialog-uuid :generating?] false)
       (assoc-in [:dialogs dialog-uuid :exchanges exchange-uuid :aborted?] true))))

(reg-event-fx
 :get-answer
 ollama-interceptors
 (fn [{:keys [db]} [_ {:keys [prompt context set-abort!] :as payload}]]
   {:fetch-stream {:url (str api-base "/api/generate")
                   :method :post
                   :body (cond-> {:model (:selected-model db)
                                  :prompt prompt}
                           (some? context)
                           (assoc :context context))
                   :set-abort! set-abort!
                   :on-progress [:get-answer-progress payload]
                   :on-success [:get-answer-success payload]
                   :on-abort [:get-answer-abort payload]
                   :on-failure [:get-answer-failure payload]}}))
