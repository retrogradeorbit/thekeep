(ns thekeep.enemy
  (:require [infinitelives.pixi.sprite :as s]
            [infinitelives.utils.events :as e]
            [infinitelives.utils.boid :as b]
            [thekeep.line :as line]
            [thekeep.state :as state]
            [infinitelives.utils.vec2 :as vec2]
            [infinitelives.utils.spatial :as spatial])
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

(defn spawn [level floor-tile-locations [x y] type speed]
  (go-while (:running? @state/state)
    (let [initial-pos (vec2/vec2 x y)
          bkey [:enemy (keyword (gensym))]
          ]
      (m/with-sprite level
        [enemy1 (s/make-sprite type :scale scale :x x :y y)]
        (add! bkey enemy1)
        (spatial/add-to-spatial! :default bkey (vec2/as-vector initial-pos))
        (loop [
               boid {:mass 2.0 :pos initial-pos
                     :vel (vec2/zero) :max-force (/ speed 2) :max-speed speed}
               hitcount 20]
          (let [pos (:pos boid)
                new-boid (b/seek boid (:pos @state/state))
                new-pos (:pos new-boid)
                constrained-pos (line/constrain
                                 {:passable? (fn [x y]
                                               (floor-tile-locations [x y]))
                                  :h-edge 0.99
                                  :v-edge 0.7
                                  :minus-h-edge 0.01
                                  :minus-v-edge 0.01}
                                 (vec2/scale pos (/ 1 16))
                                 (vec2/scale new-pos (/ 1 16)))
                new-pos (vec2/scale constrained-pos 16)
                pixel-pos (vec2/scale new-pos scale)]

            ;; collided with sword?
            (let [res (spatial/query (:default @spatial/spatial-hashes)
                                     (vec2/as-vector (vec2/sub pos (vec2/vec2 32 32)))
                                     (vec2/as-vector (vec2/add pos (vec2/vec2 32 32))))
                  matched (->> res
                               keys
                               (filter #(= :sword (first %)))
                               )
                  hit (when (pos? (count matched))
                        (let [{:keys [sword]} @state/state]
                          (when sword
                            (let [enemy-sword (-> pos (vec2/sub sword) vec2/magnitude)]
                              (when (< enemy-sword 10)
                                :hit
                                )))))]


              (swap! state/state assoc :enemy new-pos)
              (spatial/move-in-spatial :default bkey
                                       (vec2/as-vector pos)
                                       (vec2/as-vector new-pos))
              (s/set-pos! enemy1 pixel-pos)

              (<! (e/next-frame))
              (let [next-boid (assoc new-boid
                                     :pos new-pos
                                     :vel (vec2/zero))]
                (if (pos? hitcount)
                  ;; live
                  (if hit
                    (recur (assoc next-boid
                                  #_ :pos #_ (vec2/add new-pos
                                                       (vec2/scale
                                                        (vec2/sub new-pos (:pos @state/state))
                                                        0.4))
                                  :vel
                                  (vec2/scale
                                   (vec2/sub new-pos (:pos @state/state))
                                   16))
                           (dec hitcount))
                    (recur next-boid
                           hitcount))

                  ;;die
                  (do
                    (spatial/remove-from-spatial :default bkey (vec2/as-vector new-pos))
                    (remove! bkey))
                  ))))

          )))
    )
  )
