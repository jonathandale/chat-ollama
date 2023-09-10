(ns ollama-ui.fx
  (:require ["@tauri-apps/api/http" :as tauri-http]
            [clojure.string :as str]
            [applied-science.js-interop :as j]
            [cljs-bean.core :refer [->clj ->js]]
            [clojure.core.async :refer [<! go timeout]]
            [cljs.core.async.interop :refer-macros [<p!]]
            [refx.alpha :refer [dispatch reg-fx]]))

(defonce tauri? (some? (j/get js/window :__TAURI__)))
(defonce fetch (if tauri? tauri-http/fetch js/fetch))

(defn request->fetch
  [{:as   request
    :keys [url body on-success on-failure]
    :or   {on-success [:http-no-on-success]
           on-failure [:http-no-on-failure]}}]
  (let [success-> #(dispatch (conj on-success (->clj %)))
        pruned (dissoc request :on-success :on-failure :url)
        options (cond-> pruned
                  (some? body)
                  (update :body #(as-> % $
                                   (->js $)
                                   (j/call js/JSON :stringify $))))]
    (-> (fetch url (->js options))
        (.then (fn [response]
                 (if (j/get response :ok)
                   (let [data (j/get response :data)]
                     (if (some? data)
                       (success-> data)
                       (-> (j/call response :json)
                           (.then success->))))
                   (dispatch (conj on-failure request)))))
        (.catch #(do
                   (js/console.log "%" %)
                   (dispatch (conj on-failure (assoc request :status 0))))))))

(defn fetch-effect [request]
  (let [seq-request-maps (if (sequential? request) request [request])]
    (doseq [request seq-request-maps]
      (request->fetch request))))

(reg-fx :fetch fetch-effect)

(defn- parse-chunk [chunk]
  (try
    (j/get (js/JSON.parse chunk) :response)
    (catch js/Error e
      (js/console.log "chunk parse error" e chunk)
      chunk)))

(defn request->fetch-stream
  [{:as   request
    :keys [url body on-success on-progress on-failure]
    :or   {on-success [:http-no-on-success]
           on-progress [:http-no-on-progress]
           on-failure [:http-no-on-failure]}}]
  (let [pruned (dissoc request :on-success :on-failure :url)
        options (cond-> pruned
                  (some? body)
                  (update :body #(as-> % $
                                   (->js $)
                                   (j/call js/JSON :stringify $))))]
    (go
      (let [response (<p! (fetch url (->js options)))]
        (if-not (j/get response :ok)
          (dispatch (conj on-failure request))
          (let [reader (-> response
                           (j/get :body)
                           (j/call :getReader))]

            (loop [data {:buffer ""
                         :idx 0}]
              (let [{:keys [done value]}
                    (j/lookup (<p! (j/call reader :read)))]
                (if done
                  (dispatch (conj on-success (:buffer data)))
                  (let [{:keys [buffer]} data
                        chunk (-> (new js/TextDecoder)
                                  (j/call :decode value))
                        text (parse-chunk chunk)
                        new-line? (= "\n" text)
                        idx (if new-line?
                              (inc (:idx data))
                              (:idx data))]
                    (dispatch (conj on-progress {:text text
                                                 :new-line? new-line?
                                                 :idx (:idx data)}))
                    (recur {:buffer (str buffer text)
                            :idx idx})))))))))))

(reg-fx :fetch-stream request->fetch-stream)
