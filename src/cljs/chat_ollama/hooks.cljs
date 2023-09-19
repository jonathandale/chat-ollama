(ns chat-ollama.hooks
  (:require [applied-science.js-interop :as j]
            [helix.hooks :refer [use-effect use-state]]))


(defn use-copy-to-clipboard []
  (let [[copied set-copied!] (use-state false)
        clipboard (j/get js/navigator :clipboard)
        copy! #(do
                 (j/call clipboard :writeText %)
                 (set-copied! true))]
    (use-effect
     [copied]
     (when copied
       (let [wait (js/setTimeout #(set-copied! false) 1500)]
         #(js/clearTimeout wait))))

    (when clipboard
      [copied copy!])))
