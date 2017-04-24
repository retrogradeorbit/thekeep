(ns thekeep.titles
  (:require [infinitelives.pixi.tilemap :as tm]
            [infinitelives.pixi.sprite :as s]
            [infinitelives.utils.events :as e]
            [infinitelives.utils.vec2 :as vec2]
            [thekeep.assets :as assets]
            [thekeep.map :as themap]
            [thekeep.state :as state]
            [cljs.core.async :refer [timeout]])
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [infinitelives.pixi.macros :as m] )
  )

(def scale 2)

(defn run []
  (go
    (swap! state/state assoc :running? false)
    (let [tile-set (tm/make-tile-set :tiles2 assets/tile-mapping [16 16])
          [floor-tile-map wall-tile-map] (themap/make-title-screen-map)

          floor-tile-results (tm/make-tile-sprites tile-set floor-tile-map)
          floor-tile-sprites (mapv second floor-tile-results)
          floor-tile-locations (into #{} (mapv first floor-tile-results))
          wall-tile-sprites (mapv second (tm/make-tile-sprites tile-set wall-tile-map))

          floor (s/make-container :children floor-tile-sprites
                                  :scale scale
                                  )

          walls (s/make-container :children wall-tile-sprites
                                  :scale scale
                                  )]

      (m/with-sprite :title
        [container (s/make-container :children [floor walls] :scale 1)]
        (loop [theta 0]
          (let [[x y] (vec2/get-xy (vec2/add (vec2/vec2 -750 -500) (vec2/rotate (vec2/vec2 300 0) theta )))]
            (s/set-pos! container (vec2/vec2
                                   ;x y
                                   (int x) (int y)
                                   ))
            (<! (e/next-frame))
            (when-not (e/is-pressed? :space)
              (recur (+ theta 0.01)))))))))
