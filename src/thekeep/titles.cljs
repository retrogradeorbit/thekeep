(ns thekeep.titles
  (:require [infinitelives.pixi.tilemap :as tm]
            [infinitelives.pixi.sprite :as s]
            [thekeep.assets :as assets]
            [thekeep.map :as themap]
            [cljs.core.async :refer [timeout]])
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [infinitelives.pixi.macros :as m] )
  )

(def scale 2)

(defn run []
  (go
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

      (m/with-sprite :tilemap
        [container (s/make-container :children [floor walls] :scale 1)]
        (<! (timeout 2000))))))
