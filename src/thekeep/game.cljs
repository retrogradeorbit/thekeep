(ns thekeep.game
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
            [thekeep.spawner :as spawner]
            [thekeep.titles :as titles]
            [thekeep.state :as state]
            [thekeep.controls :as controls]

            [cljs.core.async :refer [timeout]]
            )
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [thekeep.async :refer [go-while]]
                   [infinitelives.pixi.macros :as m]
                   [infinitelives.pixi.pixelfont :as pf])
  )

(def scale 2)

(defn depth-compare [a b]
  (cond
    (< (s/get-y a) (s/get-y b)) -1
    (< (s/get-y b) (s/get-y a)) 1
    :default 0))

(defn hearts-updater []
  (go-while (:running? @state/state)
            (m/with-sprite :score
              [score (pf/make-text :small "0" :x -100 :y -16 :scale 2 :xhandle 1.0 :yhandle 1.0)]
              (m/with-sprite-set :hearts
                [hearts (mapv #(s/make-sprite :heart :scale 2 :x (* 32 %)) (range 5))]
                (let [health (:health @state/state)
                      num (max 0 (Math/floor (/ health 20)))]
                  (loop [num num]
                    (doseq [n (range num)]
                      (s/set-visible! (hearts n) true))
                    (doseq [n (range num 5)]
                      (s/set-visible! (hearts n) false))
                    (<! (timeout 300))
                    (pf/change-text! score :small (str (:score @state/state)))
                    (let [new-health (:health @state/state)
                          new-num (max 0 (Math/floor (/ new-health 20)))]
                      (when (< new-num num)
                        (sound/play-sound :die2 0.5 false))

                      (recur new-num))))
                (<! (e/wait-frames 600))))))

(defn run []
  (go
    (enemy/empty!)
    (swap! state/state assoc
           :running? true
           :health 100
           :score 0
           :pos (vec2/vec2 (* 5 16) (* 5 16))
           )
    (sound/play-sound :startup 0.5 false)
    (let [tile-set (tm/make-tile-set :tiles2 assets/tile-mapping [16 16])
          [floor-tile-map wall-tile-map] (themap/make-tile-map)
          floor-tile-results (tm/make-tile-sprites tile-set floor-tile-map)
          floor-tile-sprites (mapv second floor-tile-results)
          floor-tile-locations (into #{} (mapv first floor-tile-results))
          wall-tile-sprites (mapv second (tm/make-tile-sprites tile-set wall-tile-map))

          floor (s/make-container :children floor-tile-sprites
                                  :scale scale
                                  )

          walls (s/make-container :children wall-tile-sprites
                                  :scale scale
                                  )
          playersprite (s/make-sprite :down :scale scale :x 0 :y 0)
          swordsprite (s/make-sprite :sword :scale scale :x 0 :y 0 :yhandle 1.3)

          ;lion (s/make-sprite :lion :scale scale :yhandle 0.8 :x (* scale 250) :y (* scale 300))
          ;fire (s/make-sprite :fire :scale scale :yhandle 0.8 :x (* scale 175) :y (* scale 150))
          chest (s/make-sprite :chest :scale scale :x (* scale 240) :y (* scale 100))
          switch (s/make-sprite :switch1 :scale scale :x (* scale 80) :y (* scale 120))

                                        ;enemy1 (s/make-sprite :enemy1 :scale scale :x (* scale 215) :y (* scale 270))

          level (s/make-container :children [chest switch ; lion fire
                                             ])
          ]

      (hearts-updater)

      (spatial/new-spatial! :default 8)


      (m/with-sprite :tilemap
        [
         container (s/make-container :children [floor walls level] :scale 1)

         ]


        (spawner/spawn level floor-tile-locations [250 350] :lion 0   :enemy1 1.0 100)
        (spawner/spawn level floor-tile-locations [700 150] :fire 700 :enemy2 2.0 250)
        (spawner/spawn level floor-tile-locations [520 650] :fire 300 :enemy3 3.0 500)
        (spawner/spawn level floor-tile-locations [920 350] :lion 300 :enemy4 4.0 750)

          ;; camera tracking

        (go-while (:running? @state/state)
          (loop [cam (:pos @state/state)]
            (let [[cx cy] (vec2/get-xy cam)]

              (s/set-pivot! container (int cx) (int cy))
                                        ;(s/set-pivot! player (/ (int (* scale cx)) scale) (/ (int (* scale cy)) scale))

              (<! (e/next-frame))
              (let [next-pos (vec2/scale (:pos @state/state) 2)
                    v (vec2/sub next-pos cam)
                    mag (vec2/magnitude-squared v)]
                (recur (vec2/add cam (vec2/scale v (* 0.0000002 mag))))))))


        (<!
         (go
           (m/with-sprite level
             [player playersprite
              sword swordsprite
              ]
             (spatial/add-to-spatial! :default [:sword] [(* 5 16) (* 5 16)])
             (spatial/add-to-spatial! :default [:player] [(* 5 16) (* 5 16)])

             (loop [pos (vec2/vec2 (* 5 16) (* 5 16))
                    bounce (vec2/zero)
                    vel (vec2/zero)
                    sword-theta 0.0
                    was-pressed? false]
               (let [
                     joy (controls/direction)

                     new-pos (vec2/add pos (vec2/scale joy 2))

                     ;; new-pos (line/constrain-circle
                     ;;          [(/ (vec2/get-x (:enemy @state)) scale)
                     ;;           (/ (vec2/get-y (:enemy @state)) scale)
                     ;;           10]
                     ;;          pos
                     ;;          new-pos)

                     res (spatial/query (:default @spatial/spatial-hashes)
                                        (vec2/as-vector (vec2/sub pos (vec2/vec2 4 4)))
                                        (vec2/as-vector (vec2/add pos (vec2/vec2 4 4))))
                     matched (->> res
                                  (filter #(= :enemy (first (first %))))
                                  )
                     hit (when (pos? (count matched))
                           (let [[xe ye] (-> matched first second)
                                 epos (vec2/vec2 xe ye)
                                 player->enemy (vec2/sub epos pos)]
                             (vec2/scale player->enemy -1)))


                     new-pos (vec2/add new-pos bounce)


                     constrained-pos (line/constrain
                                      {:passable? (fn [x y]
                                        ;(js/console.log "pass?" x y)
                                        ;(js/console.log (floor-tile-locations [x y]))
                                                    (floor-tile-locations [x y]))
                                       :h-edge 0.99
                                       :v-edge 0.7
                                       :minus-h-edge 0.01
                                       :minus-v-edge 0.01}
                                      (vec2/scale pos (/ 1 16))
                                      (vec2/scale new-pos (/ 1 16))
                                      )

                     new-pos (vec2/scale constrained-pos 16)

                     pixel-pos (vec2/scale new-pos scale)
                     ]



                 ;; handle hit
                 (when hit
                   (sound/play-sound :hurt-low 0.5 false)
                   (swap! state/state update :health - 3))

                 (swap! state/state assoc :pos new-pos)
                 (s/set-pos! player pixel-pos)
                 (spatial/move-in-spatial :default [:sword] (vec2/as-vector pos) (vec2/as-vector new-pos))
                 (spatial/move-in-spatial :default [:player] (vec2/as-vector pos) (vec2/as-vector new-pos))

                 (if (controls/fire?)
                   (do
                     ;; spinning
                     (js/console.log "!" was-pressed?)
                     (when-not was-pressed?
                       (sound/play-sound :swoosh 0.5 false))
                     (s/set-pos! sword pixel-pos)
                     (s/set-rotation! sword sword-theta)
                     (swap! state/state assoc :sword (vec2/add new-pos (-> (vec2/vec2 0 -20) (vec2/rotate sword-theta)))))

                   (do
                     ;; holding
                     ;(js/console.log "-" was-pressed?)
                     (s/set-pos! sword (vec2/scale (vec2/add new-pos (vec2/vec2 5 13)) scale))
                     (s/set-rotation! sword 0)
                     (swap! state/state assoc :sword nil)))

                 (.sort (.-children level) depth-compare )
                 (let [this-press (controls/fire?)]
                   (<! (e/next-frame))
                   (when (pos? (:health @state/state))
                     (recur new-pos
                            (if hit
                              hit
                              (vec2/scale bounce 0.5))
                            vel
                            (+ sword-theta 0.2)
                            this-press
                            ))))))))
        (sound/play-sound :lose2 0.5 false)
        (<! (timeout 100))
        (sound/play-sound :lose-low 0.5 false)
        (<! (timeout 100))
        (sound/play-sound :lose-high 0.5 false)
        (<! (timeout 5000))))))
