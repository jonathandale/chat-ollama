(ns ollama-ui.events
  (:require [applied-science.js-interop :as j]
            [cljs.spec.alpha :as s]
            [ollama-ui.db :refer [default-db]]
            [refx.alpha :refer [->interceptor reg-event-db reg-event-fx]]
            [refx.interceptors :refer [after]]))

(defonce api-base "http://127.0.0.1:11434")
(defonce wait-for 2000)
(defonce wait-multiplier 1.25)
(defonce wait-max (* 1000 60 5))

(defn check-and-throw
  "Throws an exception if `db` doesn't match the Spec `a-spec`."
  [a-spec db]
  (when-not (s/valid? a-spec db)
    (throw (ex-info (str "spec check failed: " (s/explain-str a-spec db)) {}))))

(def check-spec-interceptor (after (partial check-and-throw :ollama-ui.db/db)))

(def offline-interceptor
  (->interceptor
   :id :offline?
   :after  (fn [{:keys [coeffects] :as context}]

             (let [{:keys [event]} coeffects
                   [_ wait {:keys [url status]}] event]
               (if (and (some? url)
                        (zero? status))
                 (let [new-wait (* (or wait wait-for) wait-multiplier)]
                   (cond-> context
                     true
                     (assoc-in [:coeffects :db :ollama-offline?] true)

                     (< new-wait wait-max)
                     (update-in [:effects :fx]
                                conj
                                [:dispatch-later {:ms new-wait
                                                  :dispatch [:get-models new-wait]}])))
                 context)))))

(def ollama-interceptors
  [offline-interceptor
   check-spec-interceptor])

(reg-event-db
 :initialise-db
 ollama-interceptors
 (fn [_ _]
   default-db))

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
   (let [selected-model (:name (first models))]
     {:db (assoc db
                 :models models
                 :ollama-offline? false)
      :dispatch-n (into [(when (nil? (:selected-model db))
                           [:set-selected-model selected-model])]
                        (mapv (fn [model]
                                [:new-dialog
                                 (cond-> {:model-name (:name model)}
                                   (= selected-model (:name model))
                                   (assoc :set-selected? true))])
                              models))})))

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

;; DIALOGS

(reg-event-db
 :set-selected-dialog
 ollama-interceptors
 (fn [db [_ dialog-uuid]]
   (assoc db :selected-dialog dialog-uuid)))

(reg-event-fx
 :new-dialog
 ollama-interceptors
 (fn [{:keys [db]} [_ {:keys [model-name set-selected?]}]]
   (let [new-uuid (str (random-uuid))
         timestamp (j/call js/Date :now)]
     (cond-> {:db (-> db
                      (assoc-in [:dialogs new-uuid]
                                {:uuid new-uuid
                                 :name model-name
                                 :timestamp timestamp}))}
       set-selected?
       (assoc :dispatch [:set-selected-dialog new-uuid])))))

;; PROMPTS
(reg-event-fx
 :send-prompt
 ollama-interceptors
 (fn [{:keys [db]} [_ {:keys [selected-dialog prompt]}]]
   (let [new-uuid (str (random-uuid))
         timestamp (j/call js/Date :now)]
     {:db (assoc-in db [:dialogs selected-dialog :exchanges new-uuid]
                    {:prompt prompt
                     :timestamp timestamp})
      :dispatch [:get-answer {:prompt prompt
                              :dialog-uuid selected-dialog
                              :exchange-uuid new-uuid}]})))

(reg-event-fx
 :get-answer-success
 ollama-interceptors
 (fn [_ [_ _ _]]
   {}))

(reg-event-fx
 :get-answer-progress
 ollama-interceptors
 (fn [{:keys [db]}
      [_
       {:keys [dialog-uuid exchange-uuid]}
       {:keys [text idx new-line?]}]]
   {:db (if new-line?
          db
          (update-in db [:dialogs dialog-uuid :exchanges exchange-uuid :answer idx]
                     (fn [current]
                       (str current text))))}))

(reg-event-db
 :get-answer-failure
 ollama-interceptors
 (fn [db [_ _ {:keys [status]}]]
   (assoc db :ollama-offline? (zero? status))))

(reg-event-fx
 :get-answer
 ollama-interceptors
 (fn [{:keys [db]} [_ {:keys [prompt] :as payload}]]
   {:fetch-stream {:url (str api-base "/api/generate")
                   :method :post
                   :body {:model (:selected-model db)
                          :prompt prompt}
                   :on-progress [:get-answer-progress payload]
                   :on-success [:get-answer-success payload]
                   :on-failure [:get-answer-failure payload]}}))
