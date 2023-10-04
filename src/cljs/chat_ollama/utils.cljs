(ns chat-ollama.utils
  (:require [cognitect.transit :as t]
            [applied-science.js-interop :as j]))


(defn debounce [f delay-ms]
  (let [timer (atom nil)]
    (fn [& args]
      (when @timer (js/clearTimeout @timer))
      (reset! timer (js/setTimeout #(apply f args) delay-ms)))))

(defn throttle [f interval-ms]
  (let [timeout (atom nil)
        fire? (atom false)
        stored-args (atom [])
        fire (fn fire []
               (reset! timeout
                       (js/setTimeout #(do
                                         (reset! timeout nil)
                                         (when @fire?
                                           (reset! fire? false)
                                           (fire)))
                                      interval-ms))
               (apply f @stored-args))]
    (fn [& args]
      (reset! stored-args args)
      (if @timeout
        (reset! fire? true)
        (fire)))))

(defn local-storage-set! [k v]
  (try
    (let [w (t/writer :json)
          tv (t/write w v)]
      (-> (j/get js/window :localStorage)
          (j/call :setItem k tv)))
    (catch js/Error e
      (js/console.error e)
      (throw e))))

(defn local-storage-get [k]
  (try
    (let [r (t/reader :json)]
      (as-> (j/get js/window :localStorage) $
        (j/call $ :getItem k)
        (t/read r $)))
    (catch js/Error e
      (js/console.error e)
      (throw e))))
