(ns ollama-ui.events
  (:require [refx.alpha :refer [reg-event-db reg-event-fx]]
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

(reg-event-db
 :initialise-db
 [check-spec-interceptor]
 (fn [_ _]
   default-db))

;; GET MODELS
(reg-event-db
 :get-models-success
 [check-spec-interceptor]
 (fn [db [_ {:keys [models]}]]
   (assoc db :models models)))

(reg-event-db
 :get-models-failure
 [check-spec-interceptor]
 (fn [db [_ result]]
   db))

(reg-event-fx
 :get-models
 [check-spec-interceptor]
 (fn []
   {:http-xhrio {:method :get
                 :uri (str api-base "/api/tags")
                 :format (json-request-format)
                 :response-format (json-response-format {:keywords? true})
                 :on-success [:get-models-success]
                 :on-failure [:get-models-failure]}}))
