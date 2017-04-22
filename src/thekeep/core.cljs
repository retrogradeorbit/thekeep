(ns thekeep.core
  (:require [infinitelives.pixi.canvas :as c]
            [infinitelives.pixi.resources :as r]
            [infinitelives.pixi.texture :as t]
            [infinitelives.pixi.sprite :as s]
            [infinitelives.pixi.tilemap :as tm]

            [infinitelives.utils.events :as e]
            [infinitelives.utils.spatial :as spatial]
            [infinitelives.utils.vec2 :as vec2]
            [infinitelives.utils.gamepad :as gp]

            [infinitelives.utils.boid :as b]

            [thekeep.assets :as assets]
            [thekeep.line :as line]
            [thekeep.map :as themap]
            [thekeep.enemy :as enemy]

            [cljs.core.async :refer [timeout]]
            )
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [infinitelives.pixi.macros :as m] ))

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
  (c/init {:layers [:bg :tilemap :player :hearts :ui]
           :background bg-colour
           :expand true
           :translate {:hearts [32 -32]}
           :origins {:top-text :top
                     :damage :bottom-right
                     :hearts :bottom-left
                     :tilemap :top-left}}))

(def scale 2)

(defonce state (atom
                {:pos
                 (vec2/vec2 (* 5 16) (* 5 16))

                 :health 100

                 :sword nil

                 :enemy (vec2/zero)

                 :enemies []

                 }))

(defn depth-compare [a b]
  (cond
    (< (s/get-y a) (s/get-y b)) -1
    (< (s/get-y b) (s/get-y a)) 1
    :default 0))

(defonce main
  (go
    (<! (r/load-resources canvas :ui
                          ["img/tiles2.png"]))

    (t/load-sprite-sheet!
     (r/get-texture :tiles2 :nearest)
     assets/hero-mapping)

    (let [tile-set (tm/make-tile-set :tiles2 assets/tile-mapping [16 16])
          [floor-tile-map wall-tile-map] (do
                                           (js/console.log "start")
                                           (let [mm (themap/make-tile-map)]
                                             (js/console.log "stop")
                                             mm)
                                           )
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
          player (s/make-sprite :down :scale scale :x 0 :y 0)
          sword (s/make-sprite :sword :scale scale :x 0 :y 0 :yhandle 1.3)

          lion (s/make-sprite :lion :scale scale :yhandle 0.8 :x (* scale 250) :y (* scale 300))
          fire (s/make-sprite :fire :scale scale :yhandle 0.8 :x (* scale 175) :y (* scale 150))
          chest (s/make-sprite :chest :scale scale :x (* scale 240) :y (* scale 100))
          switch (s/make-sprite :switch1 :scale scale :x (* scale 80) :y (* scale 120))

                                        ;enemy1 (s/make-sprite :enemy1 :scale scale :x (* scale 215) :y (* scale 270))

          level (s/make-container :children [chest switch player sword lion fire])
          ]


      (js/console.log tile-set)

      (go
        (m/with-sprite-set :hearts
          [hearts (mapv #(s/make-sprite :heart :scale 2 :x (* 32 %)) (range 5))]
          (loop []
            (let [health (:health @state)
                  num (max 0 (Math/floor (/ health 20)))]
              (js/console.log num)
              (doseq [n (range num)]
                (s/set-visible! (hearts n) true))
              (doseq [n (range num 5)]
                (s/set-visible! (hearts n) false))
              (<! (timeout 300))
              (recur)))


          (<! (e/wait-frames 600))
          ))

                                        ;(js/console.log tile-map)

                                        ;(js/console.log tile-sprites)
      (spatial/new-spatial! :default 8)
      (m/with-sprite :tilemap
        [
         container (s/make-container :children [floor walls level] :scale 1)

         ]

        ;; enemys
        (go (while true
              (<! (enemy/spawn level state floor-tile-locations [220 270]))))


        ;; camera tracking
        (go

          (loop [cam (:pos @state)]
            (let [[cx cy] (vec2/get-xy cam)]

              (s/set-pivot! container (int cx) (int cy))
                                        ;(s/set-pivot! player (/ (int (* scale cx)) scale) (/ (int (* scale cy)) scale))

              (<! (e/next-frame))
              (let [next-pos (vec2/scale (:pos @state) 0.5)
                    v (vec2/sub next-pos cam)
                    mag (vec2/magnitude-squared v)]
                (recur (vec2/add cam (vec2/scale v (* 0.00001 mag))))))))

        (spatial/add-to-spatial! :default [:sword] [(* 5 16) (* 5 16)])
        (spatial/add-to-spatial! :default [:player] [(* 5 16) (* 5 16)])

        (loop [pos (vec2/vec2 (* 5 16) (* 5 16))
               bounce (vec2/zero)
               vel (vec2/zero)
               sword-theta 0.0]
          (let [
                joy (vec2/vec2 (or (gp/axis 0)
                                   (cond (e/is-pressed? :left) -1
                                         (e/is-pressed? :right) 1
                                         :default 0) )
                               (or (gp/axis 1)
                                   (cond (e/is-pressed? :up) -1
                                         (e/is-pressed? :down) 1
                                         :default 0)
                                   ))

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
              (swap! state update :health - 3))

            (swap! state assoc :pos new-pos)
            (s/set-pos! player pixel-pos)
            (spatial/move-in-spatial :default [:sword] (vec2/as-vector pos) (vec2/as-vector new-pos))
            (spatial/move-in-spatial :default [:player] (vec2/as-vector pos) (vec2/as-vector new-pos))

            (if (e/is-pressed? :z)
              (do
                ;; spinning
                (s/set-pos! sword pixel-pos)
                (s/set-rotation! sword sword-theta)
                (swap! state assoc :sword (vec2/add new-pos (-> (vec2/vec2 0 -20) (vec2/rotate sword-theta)))))

              (do
                ;; holding
                (s/set-pos! sword (vec2/scale (vec2/add new-pos (vec2/vec2 5 13)) scale))
                (s/set-rotation! sword 0)
                (swap! state assoc :sword nil)))

            (.sort (.-children level) depth-compare )

            (<! (e/next-frame))
            (when (pos? (:health @state))
              (recur new-pos
                     (if hit
                       hit
                       (vec2/scale bounce 0.5))
                     vel
                     (+ sword-theta 0.2)))))
        )



      )

    ))

#_
(js/console.log
 )
