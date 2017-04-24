(ns thekeep.core
  (:require [infinitelives.pixi.canvas :as c]
            [infinitelives.pixi.resources :as r]
            [infinitelives.pixi.texture :as t]
            [infinitelives.pixi.sprite :as s]
            [infinitelives.pixi.tilemap :as tm]
            [infinitelives.pixi.pixelfont :as pf]

            [infinitelives.utils.events :as e]
            [infinitelives.utils.spatial :as spatial]
            [infinitelives.utils.vec2 :as vec2]
            [infinitelives.utils.gamepad :as gp]
            [infinitelives.utils.sound :as sound]

            [infinitelives.utils.boid :as b]

            [thekeep.assets :as assets]
            [thekeep.line :as line]
            [thekeep.map :as themap]
            [thekeep.enemy :as enemy]
            [thekeep.game :as game]
            [thekeep.titles :as titles]

            [cljs.core.async :refer [timeout]]
            )
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [infinitelives.pixi.macros :as m]
                   [infinitelives.pixi.pixelfont :as pf]))

(enable-console-print!)

(println "This text is printed from src/thekeep/core.cljs. Go ahead and edit it and see reloading in action.")

;; define your app data so that it doesn't get over-written on reload

(defonce app-state (atom {:text "Hello world!"}))

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
  )

;(defonce bg-colour 0x52c0e5)
(defonce bg-colour 0x2F283A)

(defonce canvas
  (c/init {:layers [:bg :tilemap :player :hearts :title :ui :score]
           :background bg-colour
           :expand true
           :translate {:hearts [32 -32]}
           :origins {:top-text :top
                     :damage :bottom-right
                     :hearts :bottom-left
                     :score :bottom-right
                     :tilemap :top-left
                     :title :center
                     :ui :center}}))

(defonce main
  (go
    (<! (r/load-resources canvas :ui
                          ["img/tiles2.png"
                           "img/font.png"
                           "sfx/1up.ogg"
                           "sfx/blip1.ogg"
                           "sfx/boop-low.ogg"
                           "sfx/boop-mid.ogg"
                           "sfx/die1.ogg"
                           "sfx/die2.ogg"
                           "sfx/hurt-low.ogg"
                           "sfx/hurt-mid.ogg"
                           "sfx/hurt-mid-2.ogg"
                           "sfx/lose1.ogg"
                           "sfx/lose2.ogg"
                           "sfx/lose-high.ogg"
                           "sfx/lose-low.ogg"
                           "sfx/startup.ogg"
                           "sfx/swoosh.ogg"
                           ]))

    (pf/pixel-font :small "img/font.png" [10 78] [220 108]
                   :chars ["ABCDEFGHIJKLMNOPQRSTUVWXYZ"
                           "abcdefghijklmnopqrstuvwxyz"
                           "0123456789"]
                   :kerning {"fo" -2  "ro" -1 "la" -1 }
                   :space 5)


    (t/load-sprite-sheet!
     (r/get-texture :tiles2 :nearest)
     assets/hero-mapping)

    (while true
      (<! (titles/run))
      (<! (game/run)))))
