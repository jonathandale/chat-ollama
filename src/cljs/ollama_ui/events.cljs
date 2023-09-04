(ns ollama-ui.events
  (:require [refx.alpha :refer [reg-event-db reg-event-fx ->interceptor]]
            [ajax.core :refer [json-request-format json-response-format]]
            [ollama-ui.db :refer [default-db]]
            [refx.interceptors :refer [after]]
            [cljs.spec.alpha :as s]))

(defonce api-base "http://127.0.0.1:11434")

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
                   [_ {:keys [uri status]}] event]
               (if (and (some? uri)
                        (zero? status))
                 (assoc-in context [:coeffects :db :ollama-offline?] true)
                 context)))))

(def ollama-interceptors
  [offline-interceptor
   check-spec-interceptor])

(reg-event-db
 :initialise-db
 ollama-interceptors
 (fn [_ _]
   default-db))

;; GET MODELS
(reg-event-db
 :get-models-success
 ollama-interceptors
 (fn [db [_ {:keys [models]}]]
   (assoc db :models models)))

(reg-event-db
 :get-models-failure
 ollama-interceptors
 (fn [db [_ {:keys [status]}]]
   (if (zero? status)
     (assoc db :ollama-offline? true)
     db)))

(reg-event-fx
 :get-models
 ollama-interceptors
 (fn []
   {:http-xhrio {:method :get
                 :uri (str api-base "/api/tags")
                 :format (json-request-format)
                 :response-format (json-response-format {:keywords? true})
                 :on-success [:get-models-success]
                 :on-failure [:get-models-failure]}}))
