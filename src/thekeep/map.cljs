(ns thekeep.map
  (:require [cljs.core.match :refer-macros [match]]
            [infinitelives.utils.math :refer [rand-between]])
  )


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

(defn draw-floor-box2 [m [x y] [w h]]
  (draw-floor-box m [(max 1 (+ x (rand-between -1 1))) (max 1 (+ y (rand-between -1 1)))] [w h]))

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

(defn make-floor-walls [floor-tile-map]
  [(-> floor-tile-map
       (remap-keymap remap-expand))
   (-> floor-tile-map
       (remap-keymap remap)
       (remap-keymap remap-2)
       (remap-keymap scatter-rubble)
       (remap-keymap scatter-walldec))])

(defn make-tile-map []
  (let [floor-tile-map
        (-> (make-map-empty [70 60])

            ;; room 1
            (draw-floor-box2 [4 4] [6 7])
            (draw-floor-box2 [6 5] [9 7])
            (draw-floor-box2 [10 5] [7 8])
            (draw-floor-box2 [14 4] [4 5])

            ;; corridor
            (draw-floor-box2 [13 12] [1 7])

            ;; room 2
            (draw-floor-box2 [4 18] [15 4])
            (draw-floor-box2 [14 19] [8 6])
            (draw-floor-box2 [16 23] [5 3])
            (draw-floor-box2 [21 23] [1 1])

            ;; corridor
            (draw-floor-box2 [20 22] [33 1])
            (draw-floor-box2 [35 19] [1 14])
            (draw-floor-box2 [42 12] [1 13])

            ;; room 3
            (draw-floor-box2 [37 8] [10 5])
            (draw-floor-box2 [39 7] [8 6])
            (draw-floor-box2 [33 5] [4 4])
            (draw-floor-box2 [35 6] [7 3])

            ;; room 4
            (draw-floor-box2 [50 15] [10 10])

            ;; room 5
            (draw-floor-box2 [30 30] [10 10])
            )]
    (make-floor-walls floor-tile-map)
    )
  )


(def title
  [
   "                             "
   "   ...... ..                 "
   "     ..   ..     ....        "
   "     ..   ....  ..  ..       "
   "     ..   .. .. .....        "
   "     ..   .. .. ..           "
   "     .    .. .   ....        "
   "                             "
   "                             "
   " .  ..                .....  "
   " . ..    ....         ..  .. "
   " ...    ..  .   ....  ..  .. "
   " ...    .....  ..  .  .....  "
   " . ..   ..     .....  ..     "
   " .  ..   ....  ..     ..     "
   " .   ..         ....  ..     "
   "                             "
 ])

(def title-map
  (mapv
   (fn [line] (mapv #(when (= % ".") :floor) line))
   title))

(defn doubleup [m]
  (into []
        (apply concat (for [x m] [x x]))))

(defn x2 [m]
  (into []
        (for [l (doubleup m)]
          (doubleup l)
          )))

(defn make-title-screen-map []

  (make-floor-walls (x2 title-map)))
