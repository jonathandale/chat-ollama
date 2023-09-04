(ns ollama-ui.views
  (:require [ollama-ui.lib :refer [defnc]]
            [helix.core :refer [$]]
            [helix.hooks :refer [use-effect]]
            [refx.alpha :refer [use-sub dispatch]]))

(defn- b->gb [bytes]
  (.toFixed (/ bytes 1024 1024 1024) 2))

(defnc OllamaIcon []
  ($ :svg {:width 27 :height 36 :viewBox "0 0 27 36" :class ["fill-white"] :xmlns "http://www.w3.org/2000/svg"}
     ($ :path {:class ["fill-inherit"] :d "M26.2847 36H24.2126C24.648 34.4441 24.638 32.9302 24.1825 31.4582C23.9378 30.6686 22.9167 29.5638 23.4948 28.7713C25.4604 26.0722 25.1 23.1459 23.5774 20.4347C23.3942 20.1089 23.3732 19.68 23.5969 19.4042C25.1025 17.5523 25.3818 15.5277 24.4348 13.3302C23.6615 11.5329 21.8611 10.0644 19.834 10.1311C19.4837 10.1422 19.1488 10.1548 18.8295 10.169C18.7478 10.1726 18.6671 10.151 18.5979 10.1072C18.5286 10.0633 18.4743 9.99926 18.4421 9.92348C17.7534 8.29992 16.5436 7.24264 14.8128 6.75163C12.4103 6.06967 9.75404 7.38964 8.69544 9.63252C8.53478 9.97198 8.49273 10.172 8.04827 10.1569C7.46416 10.1356 6.86054 10.0796 6.31247 10.1917C2.55406 10.9616 0.951894 15.6095 2.89642 18.7238C3.35589 19.4588 4.02409 19.577 3.38442 20.6408C1.91439 23.0898 1.72069 26.4056 3.54959 28.7364C4.17725 29.5366 3.01954 30.9141 2.79431 31.7673C2.43393 33.1343 2.46046 34.5452 2.87389 36H0.792728C0.216129 33.9905 0.420341 31.3839 1.49546 29.5366C1.52049 29.4941 1.51948 29.4517 1.49246 29.4093C-0.295903 26.6542 -0.163766 23.011 1.28374 20.1528C1.30676 20.1064 1.30426 20.0614 1.27623 20.018C-0.492608 17.3356 -0.422034 13.7273 1.48345 11.1404C1.90388 10.5706 2.55556 9.97804 3.10663 9.42187C3.14167 9.3855 3.15468 9.34205 3.14567 9.29154C2.74926 7.18404 2.82885 5.09675 3.38442 3.02966C3.7463 1.68393 4.75535 -0.0633945 6.28694 0.00177024C8.63989 0.101791 9.5183 3.69798 9.64293 5.51653C9.64362 5.527 9.64694 5.53712 9.65255 5.54595C9.65817 5.55478 9.66592 5.56203 9.67506 5.56703C9.68421 5.57202 9.69446 5.57459 9.70486 5.57451C9.71525 5.57442 9.72546 5.57168 9.73452 5.56654C12.2541 4.11877 14.7728 4.12433 17.2904 5.58321C17.3045 5.59149 17.3205 5.596 17.3368 5.59631C17.3532 5.59663 17.3693 5.59273 17.3837 5.585C17.3982 5.57726 17.4104 5.56594 17.4193 5.5521C17.4282 5.53827 17.4334 5.52237 17.4345 5.50592C17.5351 3.94045 18.0442 1.97793 19.1403 0.800418C19.8791 0.0063167 20.9467 -0.242219 21.8656 0.274553C24.3808 1.68999 24.3597 6.99107 23.9318 9.32488C23.9228 9.37337 23.9358 9.41378 23.9708 9.44611C24.5234 9.96642 24.8818 10.3119 25.0459 10.4827C27.4139 12.9499 27.5971 17.1007 25.7727 19.9634C25.7457 20.0058 25.7437 20.0493 25.7667 20.0937C27.2232 22.967 27.3929 26.6633 25.567 29.4305C25.5523 29.453 25.5445 29.4793 25.5445 29.5063C25.5445 29.5332 25.5523 29.5595 25.567 29.582C25.9103 30.1013 26.1591 30.6732 26.3133 31.2975C26.7067 32.8938 26.6972 34.4613 26.2847 36ZM6.30496 2.09917C6.28664 2.08243 6.26284 2.07325 6.23851 2.07353C6.21418 2.07381 6.19119 2.08354 6.17432 2.10068C4.88899 3.39337 4.81241 6.6016 5.05266 8.2686C5.06167 8.33225 5.0967 8.3565 5.15777 8.34134C5.87351 8.16656 6.59977 8.06351 7.33653 8.03219C7.38258 8.03017 7.42112 8.01148 7.45215 7.97612C7.60831 7.79628 7.6884 7.65686 7.6924 7.55785C7.75397 6.00602 7.51522 3.22515 6.30496 2.09917ZM20.9692 2.16282C20.9455 2.13986 20.9171 2.12231 20.8861 2.11135C20.8551 2.10039 20.8221 2.09628 20.7893 2.0993C20.7566 2.10231 20.7249 2.11239 20.6963 2.12883C20.6678 2.14528 20.6431 2.16772 20.6238 2.19464C19.5728 3.66918 19.3325 5.7393 19.373 7.50026C19.376 7.62049 19.4536 7.7781 19.6058 7.97309C19.6358 8.01249 19.6754 8.0327 19.7244 8.03371C20.4762 8.0529 21.214 8.16454 21.9377 8.36862C21.9463 8.37109 21.9553 8.37169 21.9642 8.37037C21.973 8.36905 21.9815 8.36585 21.989 8.36098C21.9965 8.3561 22.003 8.34967 22.0078 8.34211C22.0127 8.33454 22.0159 8.32602 22.0173 8.3171C22.2305 6.76527 22.193 3.34791 20.9692 2.16282Z"})
     ($ :path {:class ["fill-inherit"] :d "M17.7619 16.7324C20.6734 19.5785 18.382 23.4823 14.7828 23.7339C14.088 23.7824 13.2632 23.7854 12.3082 23.743C9.77206 23.6308 7.31701 21.3319 8.1744 18.5859C9.41019 14.6335 15.0636 14.094 17.7619 16.7324ZM17.3144 18.3994C16.1056 16.3687 13.2242 16.0505 11.3097 16.9976C10.2721 17.5114 9.30958 18.5859 9.39067 19.8149C9.59638 22.9625 14.8353 22.8216 16.5021 21.7213C17.6778 20.9439 18.0352 19.6118 17.3144 18.3994Z"})
     ($ :path {:class ["fill-inherit"] :d "M5.48879 16.0938C5.08912 16.7897 5.27577 17.6526 5.9057 18.0211C6.53563 18.3896 7.3703 18.1243 7.76997 17.4284C8.16965 16.7326 7.98299 15.8697 7.35306 15.5012C6.72313 15.1326 5.88847 15.398 5.48879 16.0938Z"})
     ($ :path {:class ["fill-inherit"] :d "M19.3058 17.4371C19.711 18.1287 20.5476 18.3861 21.1743 18.012C21.8011 17.6379 21.9806 16.774 21.5753 16.0824C21.1701 15.3908 20.3335 15.1334 19.7068 15.5075C19.0801 15.8816 18.9005 16.7455 19.3058 17.4371Z"})
     ($ :path {:class ["fill-inherit"] :d "M12.8848 19.6027C12.4744 19.4593 12.2697 19.1708 12.2707 18.7374C12.2707 18.6914 12.2867 18.6473 12.3157 18.6131C12.6776 18.1812 13.0575 18.3025 13.4389 18.5662C13.4601 18.5809 13.4855 18.5883 13.5112 18.5872C13.5369 18.5861 13.5616 18.5766 13.5815 18.5601C13.8668 18.3313 14.3083 18.2085 14.5891 18.5146C15.0591 19.0284 14.4329 19.4042 14.077 19.6694C14.0619 19.6809 14.05 19.6962 14.0424 19.7137C14.0348 19.7312 14.0317 19.7504 14.0335 19.7694L14.1146 20.565C14.1196 20.6125 14.1071 20.655 14.077 20.6923C13.8628 20.9621 13.5805 21.0399 13.2302 20.9257C12.6926 20.7499 12.9599 20.1195 12.9719 19.7285C12.9729 19.6649 12.9439 19.6229 12.8848 19.6027Z"})))

(defnc Header []
  (let [models (use-sub [:models])]

    (use-effect
     :once
     (dispatch [:get-models]))

    ($ :div {:class ["text-white" "bg-white/5"]}
       #_($ OllamaIcon)
       ($ :ul
          (when (seq models)
            (for [model models]
              ($ :li {:key (:digest model)}
                 (str (:name model) " — " (b->gb (:size model)) "GB"))))))))

(defnc Dialog []
  ($ :div {:class ["grow" "w-full" "max-w-md" "mx-auto"]}
     ($ :p "Dialog")))

(defnc Footer []
  ($ :div {:class ["max-w-md" "w-full" "mx-auto"]}
     ($ :p "Footer")))

(defnc Main []
  ($ :div {:class ["flex" "flex-col" "w-full" "h-full"]}
     ($ Header)
     ($ Dialog)
     ($ Footer)))
