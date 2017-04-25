(ns thekeep.spawner
  (:require [infinitelives.pixi.sprite :as s]
            [infinitelives.utils.events :as e]
            [infinitelives.utils.boid :as b]
            [thekeep.line :as line]
            [thekeep.enemy :as enemy]
            [thekeep.state :as state]
            [infinitelives.utils.vec2 :as vec2]
            [infinitelives.utils.spatial :as spatial]
            [cljs.core.async :refer [timeout]]
            )
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [thekeep.async :refer [go-while]]
                   [infinitelives.pixi.macros :as m] )

  )


(def scale 2)

(defonce enemies (atom {}))

(defn add! [ekey enemy]
  (swap! enemies assoc ekey enemy))

(defn remove! [ekey]
  (swap! enemies dissoc ekey))

(defn count-enemies []
  (count @enemies))

(defn spawn [level floor-tile-locations [x y] type delta enemy enemy-speed enemy-score]
  (go-while (:running? @state/state)
    (let [initial-pos (vec2/vec2 x y)
          bkey [:enemy (keyword (gensym))]
          ]
      (m/with-sprite level
        [enemy1 (s/make-sprite type :scale scale :x (* scale x) :y (* scale y) :yhandle 0.85)]
        (<! (timeout delta))
        (while true
          (enemy/spawn level floor-tile-locations [x y] enemy enemy-speed enemy-score)
          (<! (timeout 3000))
          (while (> (enemy/count-enemies) 10)
            (<! (timeout 100))))))))
