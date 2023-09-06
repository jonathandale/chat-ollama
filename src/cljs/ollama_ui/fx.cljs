(ns ollama-ui.fx
  (:require [refx.alpha :refer [dispatch reg-fx]]
            [applied-science.js-interop :as j]
            [cljs-bean.core :refer [->js ->clj]]
            ["@tauri-apps/api/http" :refer [fetch]]))

(def tauri? (some? (j/get js/window :__TAURI__)))

(defn request->fetch
  [{:as   request
    :keys [url on-success on-failure]
    :or   {on-success      [:http-no-on-success]
           on-failure      [:http-no-on-failure]}}]
  (let [success-> #(dispatch (conj on-success (->clj %)))]
    (-> ((if tauri? fetch js/fetch)
         url
         (-> request
             (dissoc :on-success :on-failure :url)
             ->js))
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

(reg-fx :http-fetch fetch-effect)
