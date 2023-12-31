(ns chat-ollama.fx
  (:require [promesa.core :as p]
            [applied-science.js-interop :as j]
            [cljs-bean.core :refer [->js ->clj]]
            [refx.alpha :refer [dispatch reg-fx]]
            [clojure.string :as str]))

(defn request->fetch
  [{:as   request
    :keys [url body on-success on-failure]
    :or   {on-success [:http-no-on-success]
           on-failure [:http-no-on-failure]}}]
  (let [success-> #(dispatch (conj on-success (js->clj % :keywordize-keys true)))
        pruned (dissoc request :on-success :on-failure :url)
        options (cond-> pruned
                  (some? body)
                  (update :body #(as-> % $
                                   (->js $)
                                   (j/call js/JSON :stringify $))))]
    (-> (js/fetch url (->js options))
        (.then (fn [response]
                 (if (j/get response :ok)
                   (let [data (j/get response :data)]
                     (if (some? data)
                       (success-> data)
                       (-> (j/call response :json)
                           (.then success->))))
                   (dispatch (conj on-failure request)))))
        (.catch #(dispatch (conj on-failure (assoc request :status 0)))))))

(defn fetch-effect [request]
  (let [seq-request-maps (if (sequential? request) request [request])]
    (doseq [request seq-request-maps]
      (request->fetch request))))

(reg-fx :fetch fetch-effect)

(defn- parse-chunk [chunk]
  (try
    (js/JSON.parse chunk)
    (catch js/Error _
      (prn "parse error!" chunk))))

(defn request->fetch-stream
  [{:keys [url body on-success on-progress on-failure signal]
    :or   {on-success [:http-no-on-success]
           on-progress [:http-no-on-progress]
           on-failure [:http-no-on-failure]}}]
  (-> (js/fetch url #js{:method "post"
                        :body (j/call js/JSON :stringify (->js body))
                        :signal signal})
      (j/call :then
              (fn [response]
                (if-not (j/get response :ok)
                  (dispatch (conj on-failure {:status 0}))
                  (let [reader (-> response
                                   (j/get :body)
                                   (j/call :getReader))]
                    #_{:clj-kondo/ignore [:unresolved-symbol]}
                    (p/loop [data {:response ""}]
                      (p/let [read (j/call reader :read)
                              {:keys [done value]} (j/lookup read)]
                        (if (and done (nil? value))
                          (dispatch (conj on-success data))
                          (let [chunk (-> (new js/TextDecoder)
                                          (j/call :decode value))
                                lines (str/split-lines chunk)
                                buffer (atom "")
                                final-result (atom nil)]

                            (doseq [line lines]
                              (let [{:keys [response] :as result} (->clj (parse-chunk line))
                                    has-value? (seq response)]
                                (if has-value?
                                  (do
                                    (swap! buffer str response)
                                    (dispatch (conj on-progress {:text response})))
                                  (reset! final-result result))))

                            (p/recur (if (some? @final-result)
                                       (merge data @final-result)
                                       (update data :response #(str % @buffer))))))))))))
      (j/call :catch
              #(when (re-find #"network error" (j/get % :message))
                 (dispatch (conj on-failure {:status 0}))))))

(reg-fx :fetch-stream
        (fn [{:keys [set-abort! on-abort] :as request}]
          (if (fn? set-abort!)
            (let [controller (new js/AbortController)
                  signal (j/get controller :signal)]
              (set-abort! (fn []
                            #(do
                               (j/call controller :abort)
                               (dispatch (conj on-abort request))
                               (set-abort! nil))))
              (request->fetch-stream (assoc request :signal signal)))
            (request->fetch-stream request))))
