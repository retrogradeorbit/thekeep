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
            [infinitelives.utils.math :refer [rand-between]]
            [infinitelives.utils.boid :as b]

            [thekeep.assets :as assets]
            [thekeep.line :as line]

            [cljs.core.match :refer-macros [match]]
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
  (c/init {:layers [:bg :tilemap :player :ui]
           :background bg-colour
           :expand true
           :origins {:top-text :top
                     :damage :bottom-right
                     :score :bottom-left}}))

(def scale 2)

(defn make-map-empty [[w h]]
  (mapv
   #(mapv (fn [_] nil) (range w))
   (range h)))

(defn draw-floor-box [m [x y] [w h]]
  (let [co-ords (for [xp (range w)
                      yp (range h)]
                  [(+ y yp) (+ x xp)]
                  )]
    (reduce (fn [acc co-ord] (assoc-in acc co-ord :floor))
            m
            co-ords)))

(defn remap [a b c
             d e f
             g h i]
  (match [a b c
          d e f
          g h i]
         ;; outer corners
         [_      _       _
          nil    nil     _
          :floor nil     _]
         :tr2

         [_      _       _
          _    nil     nil
          _ nil     :floor]
         :tl2

         [:floor nil _
          nil nil _
          _ _ _]
         :br

         [_ nil :floor
          _ nil nil
          _ _ _]
         :bl


         ;; inner corners

         [:floor :floor  _
          :floor nil     _
          _      _       _]
         :itl1

         [_ :floor :floor
          _ nil :floor
          _      _       _]
         :itr1

         [_ _ _
          :floor nil _
          :floor :floor _]
         :ibl2

         [_ _ _
          _ nil :floor
          _ :floor :floor]
         :ibr2

         ;; edges

         [_ _ _
          _ nil _
          _ :floor _]
         :t2

         [_ :floor _
          _ nil _
          _ _ _]
         :b

         [_ _ _
          :floor nil _
          _ _ _]
         :r

         [_ _ _
          _ nil :floor
          _ _ _]
         :l

         [_ _ _ _ _ _ _ _ _]
         e))

(defn remap-2 [a b c
               d e f
               g h i]
  (match [a b c
          d e f
          g h i]
         ;; tight turns
         [_ _ _
          _ :tl2 _
          _ :ibr2 _]
         :specr

         [_ _ _
          _ :tr2 _
          _ :ibl2 _]
         :specl

         ;; normal corner second bricks
         [_ _ _
          _ nil _
          _ :t2 _]
         :t1

         [_ _ _
          _ nil _
          _ :tr2 _]
         :tr1

         [_ _ _
          _ nil _
          _ :tl2 _]
         :tl1

         [_ _ _
          _ :r _
          _ :ibl2 _]
         :ibl1

         [_ _ _
          _ :l _
          _ :ibr2 _]
         :ibr1


         [:floor :itl1  _
          :floor :r     _
          _      _       _]
         :itl2

         [_ :itr1  :floor
          _ :l     :floor
          _      _       _]
         :itr2

         [_ _ _ _ _ _ _ _ _]
         e))

(defn remap-3 [a b c
             d e f
             g h i]
  (when (not= :floor e)
    e))

(defonce state (atom
                {:pos
                 (vec2/vec2 (* 5 16) (* 5 16))

                 :enemy (vec2/zero)
                 }))

(defn remap-keymap [keymap remap]
  (let [height (count keymap)
        width (count (first keymap))]
    (mapv (fn [y]
            (mapv (fn [x]
                    (let [a (get-in keymap [(dec y) (dec x)])
                          b (get-in keymap [(dec y) x])
                          c (get-in keymap [(dec y) (inc x)])
                          d (get-in keymap [y (dec x)])
                          e (get-in keymap [y x])
                          f (get-in keymap [y (inc x)])
                          g (get-in keymap [(inc y) (dec x)])
                          h (get-in keymap [(inc y) x])
                          i (get-in keymap [(inc y) (inc x)])]
                      (remap a b c d e f g h i)))
                  (range width)))
           (range height))))

(defn random-floors [m num]
  (reduce
   (fn [acc _]
     (draw-floor-box
      acc
      [(rand-between 1 30) (rand-between 1 30)]
      [(rand-between 1 10) (rand-between 1 10)]))
   m
   (range num)))

(defn remap-expand [& tiles]
  (if (some #{:floor} tiles)
    :floor
    (nth tiles 4)))

(defn scatter-rubble [a b c d e f g h i]
  (if (= :floor e)
    (if (< (rand) 0.05)
      (rand-nth [:rubble1 :rubble2 :rubble3 :rubble4])
      e)
    e))

(defn scatter-walldec [a b c d e f g h i]
  (if (= :t2 e)
    (if (< (rand) 0.15)
      (rand-nth [:walldec1 :walldec2 :walldec3 :walldec4 :portcullis :door])
      e)
    e))

(defn make-tile-map []
  (let [floor-tile-map
        (-> (make-map-empty [50 50])

            ;; room 1
            (draw-floor-box [4 4] [6 7])
            (draw-floor-box [6 5] [9 7])
            (draw-floor-box [10 5] [7 8])
            (draw-floor-box [14 4] [4 5])

            ;; corridor
            (draw-floor-box [13 13] [1 3])

            ;; room 2
            (draw-floor-box [4 16] [15 4])
            (draw-floor-box [14 17] [8 5])
            (draw-floor-box [16 22] [5 3])
            (draw-floor-box [21 22] [1 1]))

        floor-tiles (-> floor-tile-map
                        ;(remap-keymap scatter-rubble)
                        (remap-keymap remap-expand))
        wall-tiles (-> floor-tile-map

                       (remap-keymap remap)
                       (remap-keymap remap-2)
                       (remap-keymap scatter-rubble)
                       (remap-keymap scatter-walldec
                                     )
                       )]
    [floor-tiles wall-tiles]

                                        ;(remap-keymap remap-expand)
                                        ;(remap-keymap remap-expand)

                                        ;(random-floors 15)

                                        ;(remap-keymap remap-3)



    )
  )

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
                                           (let [mm (make-tile-map)]
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

          enemy1 (s/make-sprite :enemy1 :scale scale :x (* scale 215) :y (* scale 270))

          level (s/make-container :children [chest switch enemy1 player sword lion fire])
          ]


      (js/console.log tile-set)

                                        ;(js/console.log tile-map)

                                        ;(js/console.log tile-sprites)
      (spatial/new-spatial! :default 16)
      (m/with-sprite :tilemap
        [
         container (s/make-container :children [floor walls level] :scale 1)

         ]

        ;; enemy1
        (go
          (let [initial-pos (vec2/vec2 (* scale 215) (* scale 270))]
            (spatial/add-to-spatial! :default :enemy1 (vec2/as-vector initial-pos))
            (loop [boid {:mass 1.0 :pos initial-pos
                         :vel (vec2/zero) :max-force 1.0 :max-speed 1.0}]
              (let [pos (:pos boid)
                    new-boid (b/seek boid (:pos @state))
                    new-pos (:pos new-boid)
                    constrained-pos (line/constrain
                                     {:passable? (fn [x y]
                                                   (floor-tile-locations [x y]))
                                      :h-edge 0.99
                                      :v-edge 0.7
                                      :minus-h-edge 0.01
                                      :minus-v-edge 0.01}
                                     (vec2/scale pos (/ 1 16 scale))
                                     (vec2/scale new-pos (/ 1 16 scale)))
                    constrained-pos (vec2/scale constrained-pos 16)
                    new-pos (vec2/scale constrained-pos scale)]

                ;; collided with sword?
                (let [res (spatial/query (:default @spatial/spatial-hashes)
                                        (vec2/as-vector (vec2/sub pos (vec2/vec2 16 16)))
                                        (vec2/as-vector (vec2/add pos (vec2/vec2 16 16))))
                      matched (->> res
                         keys
                         (filter #(= :sword %))
                         )]
                  (when true ;(pos? (count matched))
                    (js/console.log (count matched) res
                                    (str (:default @spatial/spatial-hashes))))
                  )

                (swap! state assoc :enemy new-pos)
                (spatial/move-in-spatial :default :enemy1
                                       (vec2/as-vector pos)
                                       (vec2/as-vector new-pos))
                (s/set-pos! enemy1 new-pos)

                (<! (e/next-frame))
                (recur (assoc new-boid
                              :pos new-pos
                              :vel (vec2/zero))))

              ))
          )


        ;; camera tracking
        (go

          (loop [cam (:pos @state)]
            (let [[cx cy] (vec2/get-xy cam)]

              (s/set-pivot! container (int cx) (int cy))
                                        ;(s/set-pivot! player (/ (int (* scale cx)) scale) (/ (int (* scale cy)) scale))

              (<! (e/next-frame))
              (let [next-pos (:pos @state)
                    v (vec2/sub next-pos cam)
                    mag (vec2/magnitude-squared v)]
                (recur (vec2/add cam (vec2/scale v (* 0.00001 mag))))))))

        (spatial/add-to-spatial! :default :sword [(* 5 16) (* 5 16)])
          (spatial/add-to-spatial! :default :player [(* 5 16) (* 5 16)])

        (loop [pos (vec2/vec2 (* 5 16) (* 5 16))
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

                new-pos (line/constrain-circle
                         [(/ (vec2/get-x (:enemy @state)) scale)
                          (/ (vec2/get-y (:enemy @state)) scale)
                          10]
                         pos
                         new-pos)

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

                ;_ (js/console.log (str @state) pos)





                ;; new-pos (line/constrain-rects
                ;;          [[150 150 10 10]]
                ;;          pos new-pos)

                ;_ (js/console.log "newpos" new-pos)

                pixel-pos (vec2/scale new-pos scale)

                ]
            (swap! state assoc :pos pixel-pos)
            (s/set-pos! player pixel-pos)
            (spatial/move-in-spatial :default :sword (vec2/as-vector pos) (vec2/as-vector new-pos))
            (spatial/move-in-spatial :default :player (vec2/as-vector pos) (vec2/as-vector new-pos))

            (if (e/is-pressed? :z)
              (do
                ;; spinning
                (s/set-pos! sword pixel-pos)
                (s/set-rotation! sword sword-theta))

              (do
                ;; holding
                (s/set-pos! sword (vec2/scale (vec2/add new-pos (vec2/vec2 5 13)) scale))
                (s/set-rotation! sword 0)))

            (.sort (.-children level) depth-compare )

            (<! (e/next-frame))
            (recur new-pos vel
                   (+ sword-theta 0.2))))
        )



      )

    ))

#_
(js/console.log
 )
