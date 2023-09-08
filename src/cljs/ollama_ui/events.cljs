(ns ollama-ui.events
  (:require [refx.alpha :refer [reg-event-db reg-event-fx ->interceptor]]
            [ollama-ui.db :refer [default-db]]
            [refx.interceptors :refer [after]]
            [cljs.spec.alpha :as s]
            ["date-fns" :refer [formatISO getUnixTime]]))

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
   {:http-fetch {:url (str api-base "/api/tags")
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
         date (new js/Date)]
     (cond-> {:db (-> db
                      (assoc-in [:dialogs new-uuid]
                                {:uuid new-uuid
                                 :name model-name
                                 :created-at (formatISO date)
                                 :timestamp (getUnixTime date)}))}
       set-selected?
       (assoc :dispatch [:set-selected-dialog new-uuid])))))

;; PROMPTS
(reg-event-fx
 :send-prompt
 ollama-interceptors
 (fn [{:keys [db]} [_ {:keys [selected-dialog prompt]}]]
   {:db (update-in db [:dialogs selected-dialog :exchange]
                   conj {:question prompt})}))
