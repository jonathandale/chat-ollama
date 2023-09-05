(ns ollama-ui.events
  (:require [refx.alpha :refer [reg-event-db reg-event-fx ->interceptor]]
            [ajax.core :refer [json-request-format json-response-format]]
            [ollama-ui.db :refer [default-db]]
            [refx.interceptors :refer [after]]
            [cljs.spec.alpha :as s]))

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
                   [_ wait {:keys [uri status]}] event]
               (if (and (some? uri)
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
 (fn [db [_ model]]
   (assoc db :selected-model model)))

;; GET MODELS
(reg-event-fx
 :get-models-success
 ollama-interceptors
 (fn [{:keys [db]} [_ {:keys [models]}]]
   (cond-> {:db (assoc db
                       :models models
                       :ollama-offline? false)}
     (nil? (:selected-model db))
     (assoc :dispatch [:set-selected-model (first models)]))))

(reg-event-db
 :get-models-failure
 ollama-interceptors
 (fn [db [_ _wait {:keys [status]}]]
   (assoc db :ollama-offline? (zero? status))))

(reg-event-fx
 :get-models
 ollama-interceptors
 (fn [_db [_ wait]]
   {:http-xhrio {:method :get
                 :uri (str api-base "/api/tags")
                 :format (json-request-format)
                 :response-format (json-response-format {:keywords? true})
                 :on-success [:get-models-success]
                 :on-failure [:get-models-failure wait]}}))
