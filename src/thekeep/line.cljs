(ns thekeep.line
  (:require [infinitelives.utils.console :refer [log]]
            [infinitelives.utils.vec2 :as vec2]
            [cljs.core.match :refer-macros [match]]
))


(defn octant-of [x0 y0 x1 y1]
  (cond
    (and (< x0 x1)
         (< y0 y1)
         (> (- x1 x0) (- y1 y0)))
    0

    (and (< x0 x1)
         (< y0 y1)
         (<= (- x1 x0) (- y1 y0)))
    1

    (and (<= x1 x0)
         (< y0 y1)
         (< (- x0 x1) (- y1 y0)))
    2

    (and (<= x1 x0)
         (< y0 y1)
         (>= (- x0 x1) (- y1 y0)))
    3

    (and (<= x1 x0)
         (<= y1 y0)
         (> (- x0 x1) (- y0 y1)))
    4

    (and (<= x1 x0)
         (<= y1 y0)
         (<= (- x0 x1) (- y0 y1)))
    5

    (and (< x0 x1)
         (<= y1 y0)
         (< (- x1 x0) (- y0 y1)))
    6

    (and (< x0 x1)
         (<= y1 y0)
         (>= (- x1 x0) (- y0 y1)))
    7))

(defn to-zero-octant [octant x y]
  (case octant
    0 [x y]
    1 [y x]
    2 [y (- x)]
    3 [(- x) y]
    4 [(- x) (- y)]
    5 [(- y) (- x)]
    6 [(- y) x]
    7 [x (- y)]))

(defn from-zero-octant [octant x y]
  (case octant
    0 [x y]
    1 [y x]
    2 [(- y) x]
    3 [(- x) y]
    4 [(- x) (- y)]
    5 [(- y) (- x)]
    6 [y (- x)]
    7 [x (- y)]))

(defn bresenham [x0 y0 x1 y1]
  (let [octant (octant-of x0 y0 x1 y1)
        [x0 y0] (to-zero-octant octant x0 y0)
        [x1 y1] (to-zero-octant octant x1 y1)]
    (for [x (range x0 (inc x1))]
      (from-zero-octant
       octant x (+ y0
                   (* (- x x0)
                      (/ (- y1 y0)
                         (- x1 x0))))))))


(defn intify [s]
  (for [[x y] s]
    [(int (+ 0.5 x))
     (int (+ 0.5 y))]))

(defn cover-all [s]
  (apply
   concat
   (for [[x y] s]
     (cond
       (= 0.5 (mod x 1)) [[(int x) y] [(inc (int x)) y]]
       (= 0.5 (mod y 1)) [[x (int y)] [x (inc (int y))]]
       :default [[x y]]))))

(defn intersect-x [x0 y0 x1 y1 y]
  (+ x0
     (* (- y y0)
        (/ (- x1 x0)
           (- y1 y0)))))

(defn intersect-y [x0 y0 x1 y1 x]
  (+ y0
     (* (- x x0)
        (/ (- y1 y0)
           (- x1 x0)))))

(defn cell-coverage [x0 y0 x1 y1
                     x-fn y-fn
                     dy-v-fn
                     dx-h-fn]
  (loop [x (Math/floor x0) y (Math/floor y0) s [[x y]]]
    (if (and (= x (Math/floor x1)) (= y (Math/floor y1)))
      s
      (let [top-bottom-x (intersect-x x0 y0 x1 y1 (y-fn y))
            left-right-y (intersect-y x0 y0 x1 y1 (x-fn x))]
        (cond
          ;; cuts top/bottom
          (<= x top-bottom-x (inc x))
          (recur x (dy-v-fn y)
                 (conj s [x (dy-v-fn y)]))

          ;; cuts left/right
          (<= y left-right-y (inc y))
          (recur (dx-h-fn x) y
                 (conj s [(dx-h-fn x) y]))

          ;; cuts perfect diagonal
          (and (= top-bottom-x (dx-h-fn x))
               (= left-right-y (dy-v-fn y)))
          (recur (dx-h-fn x) (dy-v-fn y)
                 (conj s
                       [x (dy-v-fn y)]
                       [(dx-h-fn x) y]
                       [(dx-h-fn x) (dy-v-fn y)]))

          :default (assert false "cell-coverage fatal error!"))))))

(defn cell-coverage-line-x [x0 y0 x1 y1 dx-fn]
  (loop [x (Math/floor x0)
         y (Math/floor y0)
         s [[x y]]]
    (if (= x (Math/floor x1))
      s
      (let [right-y (intersect-y x0 y0 x1 y1 (inc x))]
        (assert (<= y right-y (inc y)) "intersection out of range")
        (recur (dx-fn x) y
               (conj s [(dx-fn x) y]))))))

(defn cell-coverage-line-y [x0 y0 x1 y1 dy-fn]
  (loop [x (Math/floor x0)
         y (Math/floor y0)
         s [[x y]]]
    (if (= y (Math/floor y1))
      s
      (let [top-x (intersect-x x0 y0 x1 y1 (inc y))]
        (assert (<= x top-x (inc x)) "intersection out of range")
        (recur x (dy-fn y)
               (conj s [x (dy-fn y)]))))))

(defn all-covered [x0 y0 x1 y1]
  (match [(Math/sign (- x1 x0)) (Math/sign (- y1 y0))]

         ;; diagonals
         [1 1] (cell-coverage x0 y0 x1 y1 inc inc inc inc)
         [1 -1] (cell-coverage x0 y0 x1 y1 inc identity dec inc)
         [-1 1] (cell-coverage x0 y0 x1 y1 identity inc inc dec)
         [-1 -1] (cell-coverage x0 y0 x1 y1 identity identity dec dec)

         ;; compass directions
         [1 _] (cell-coverage-line-x x0 y0 x1 y1 inc)
         [-1 _] (cell-coverage-line-x x0 y0 x1 y1 dec)
         [_ -1] (cell-coverage-line-y x0 y0 x1 y1 dec)
         [_ 1] (cell-coverage-line-y x0 y0 x1 y1 inc)

         ;; new and old identical
         [0 0] [[(Math/floor x0) (Math/floor y0)]]
         ))

(defn vec2->parts [pos]
  (let [x (vec2/get-x pos)
        y (vec2/get-y pos)
        ix (int x)
        iy (int y)
        fx (- x ix)
        fy (- y iy)]
    [x y ix iy fx fy])
  )

(defn apply-edge-constraints [{:keys [passable?
                                      h-edge v-edge
                                      minus-h-edge minus-v-edge]}
                              oldpos newpos]
  (let [[nx ny nix niy nfx nfy] (vec2->parts newpos)
        [ox oy oix oiy ofx ofy] (vec2->parts oldpos)
        dx (- nix oix)
        dy (- niy oiy)

        <left-edge (< nfx h-edge)
        >right-edge (> nfx minus-h-edge)
        <top-edge (< nfy v-edge)
        >bottom-edge (> nfy minus-v-edge)

        pass? (boolean (passable? nix niy))
        pass-left? (boolean (passable? (dec nix) niy))
        pass-right? (boolean (passable? (inc nix) niy))
        pass-top? (boolean (passable? nix (dec niy)))
        pass-bottom? (boolean (passable? nix (inc niy)))
        pass-top-left? (boolean (passable? (dec nix) (dec niy)))
        pass-bottom-left? (boolean (passable? (dec nix) (inc niy)))
        pass-top-right? (boolean (passable? (inc nix) (dec niy)))
        pass-bottom-right? (boolean (passable? (inc nix) (inc niy)))]
    (match [<left-edge >right-edge <top-edge >bottom-edge
            pass-top-left? pass-top? pass-top-right?
            pass-left? pass? pass-right?
            pass-bottom-left? pass-bottom? pass-bottom-right?]

           ;; outer top-right corner
           [true _ _ true
            _ _ _
            true true _
            false true _]
           (let [ddx (- h-edge nfx) ddy (- nfy minus-v-edge)]
             (if (< ddx ddy)
               (vec2/vec2 (+ nix h-edge) ny)
               (vec2/vec2 nx (+ niy minus-v-edge))))

           ;; outer bottom-right corner
           [true _ true _
            false true _
            true true _
            _ _ _]
           (let [ddx (- h-edge nfx) ddy (- v-edge nfy)]
             (if (< ddx ddy)
               (vec2/vec2 (+ nix h-edge) ny)
               (vec2/vec2 nx (+ niy v-edge))))

           ;; outer top left
           [_ true _ true
            _ _ _
            _ true true
            _ true false]
           (let [ddx (- nfx minus-h-edge) ddy (- nfy minus-v-edge)]
             (if (< ddx ddy)
               (vec2/vec2 (+ nix minus-h-edge) ny)
               (vec2/vec2 nx (+ niy minus-v-edge))))

           ;; outer bottom left
           [_ true true _
            _ true false
            _ true true
            _ _ _]
           (let [ddx (- nfx minus-h-edge) ddy (- v-edge nfy)]
             (if (< ddx ddy)
               (vec2/vec2 (+ nix minus-h-edge) ny)
               (vec2/vec2 nx (+ niy v-edge))))

           ;; inner bottom left
           [true _ _ true
            _ _ _
            false true _
            _ false _]
           (vec2/vec2 (+ nix h-edge) (+ niy minus-v-edge))

           ;; inner top left
           [true _ true _
            _ false _
            false true _
            _ _ _]
           (vec2/vec2 (+ nix h-edge) (+ niy v-edge))

           ;; inner top right
           [_ true true _
            _ false _
            _ true false
            _ _ _]
           (vec2/vec2 (+ nix minus-h-edge) (+ niy v-edge))

           ;; inner bottom right
           [_ true _ true
            _ _ _
            _ true false
            _ false _]
           (vec2/vec2 (+ nix minus-h-edge) (+ niy minus-v-edge))

           ;; right edge
           [true _ _ _
            _ _ _
            false _ _
            _ _ _]
           (vec2/vec2 (+ nix h-edge) ny)

           ;; left edge
           [_ true _ _
            _ _ _
            _ _ false
            _ _ _]
           (vec2/vec2 (+ nix minus-h-edge) ny)

           ;; bottom edge
           [_ _ true _
            _ false _
            _ _ _
            _ _ _]
           (vec2/vec2 nx (+ niy v-edge))

           ;; top edge
           [_ _ _ true
            _ _ _
            _ _ _
            _ false _]
           (vec2/vec2 nx (+ niy minus-v-edge))

           ;; open space
           [_ _ _ _
            _ _ _
            _ _ _
            _ _ _]
           newpos)))

(defn intersect-diag
  "casting a line from [x0 y0] to [x1 y1], as it intersects the
  bounding square of cell [x y], where x-fn and y-fn identify which
  edges of [x y] to check, find the earliest intersection with the
  bounding square and return a vector specifying that intersection
  position."
  [x0 y0 x1 y1 x y x-fn y-fn]
  (let [top-x (intersect-x x0 y0 x1 y1 (y-fn y))
        left-y (intersect-y x0 y0 x1 y1 (x-fn x))]
    (cond (<= x top-x (inc x))
          ;; cuts top
          (vec2/vec2 top-x (y-fn y))

          ;; cuts left
          (<= y left-y (inc y))
          (vec2/vec2 (x-fn x) left-y))))

(defn intersect-compass-y
  "This is a faster form of intersect-diag that only works when the
  line is cast entirely vertical in cell space. ie, some fine lateral
  movement is allowed, but all the intersecting cells must be in a
  straight, vertical stack"
  [x0 y0 x1 y1 x y y-fn]
  (let [bottom-x (intersect-x x0 y0 x1 y1 (y-fn y))]
    (when (<= x bottom-x (inc x))
      (vec2/vec2 bottom-x (y-fn y)))))

(defn intersect-compass-x
  "Same as intersect-compass-y but in a horizontal direction"
  [x0 y0 x1 y1 x y x-fn]
  (let [right-y (intersect-y x0 y0 x1 y1 (x-fn x))]
    (when (<= y right-y (inc y))
      (vec2/vec2 (x-fn x) right-y))))

(defn intersect
  "Given a movement of a point from oldpos to newpos,
  calculate the exact position it intersects the outside
  of tile [x y]"
  [oldpos newpos x y]
  (let [[nx ny nix niy nfx nfy] (vec2->parts newpos)
        [ox oy oix oiy ofx ofy] (vec2->parts oldpos)]
    (match [(Math/sign (- nx ox)) (Math/sign (- ny oy))]
      [1 1] (intersect-diag ox oy nx ny x y identity identity)
      [-1 1] (intersect-diag ox oy nx ny x y inc identity)
      [1 -1] (intersect-diag ox oy nx ny x y identity inc)
      [-1 -1] (intersect-diag ox oy nx ny x y inc inc)
      [-1 _] (intersect-compass-x ox oy nx ny x y inc)
      [1 _] (intersect-compass-x ox oy nx ny x y identity)
      [_ -1] (intersect-compass-y ox oy nx ny x y inc)
      [_ 1] (intersect-compass-y ox oy nx ny x y identity)
      [0 0] (assert false "no movement!"))))

(defn reject
  "does the same as intersect, but instead of returning the exact
  point of intersection, returns a point slightly 'before' it (on the
  oldpos -> newpos line). Thus this point is garunteed to be in the
  neighboring tile."
  [oldpos newpos x y]
   (let [newpos (intersect oldpos newpos x y)]
     (-> (vec2/sub newpos oldpos)
        (vec2/scale 0.999)
        (vec2/add oldpos))))


(defn constrain [{:keys [passable?]
                  :as opts} oldpos newpos]
  (if (vec2/almost newpos oldpos)
    newpos
    (let [[nx ny nix niy nfx nfy] (vec2->parts newpos)
          [ox oy oix oiy ofx ofy] (vec2->parts oldpos)
          dx (- nix oix)
          dy (- niy oiy)]
      (if (and (> 2 (Math/abs dx))
               (> 2 (Math/abs dy))
               (> 2 (+ (Math/abs dx) (Math/abs dy))) ;; cant jump through diagonals
               (passable? nix niy))
        ;; small +/- 1 tile horiz/vert movements
        ;; in an open square. apply edge constraints
        (apply-edge-constraints
         opts
         oldpos newpos)

        ;; new tile collides. moving so fast got embedded in other tile.
        ;; eject drastically!
        (let [points (all-covered ox oy nx ny)]
          (loop [[[x y] & r] points]
            (if (passable? x y)
              (if (zero? (count r))
                ;; no colision found
                newpos

                ;; try next point
                (recur r))

              ;; not passable! reject from this tile
              (apply-edge-constraints opts oldpos (reject oldpos newpos x y)))))))))

(defn constrain-offset [opts offset oldpos newpos]
  (vec2/add
   offset
   (constrain opts (vec2/sub oldpos offset) (vec2/sub newpos offset))))

(defn clamp [v mn mx]
  (max
   mn
   (min v mx)))

(defn aabb-intersect-point [[cx cy hx hy] [x y]]
  (let [dx (- x cx)
        px (- hx (Math/abs dx))]
    (when (pos? px)
      (let [dy (- y cy)
            py (- hy (Math/abs dy))]
        (when (pos? py)
          (if (< px py)
            (let [sx (Math/sign dx)]
              {:delta [(* px sx) 0]
               :normal [sx 0]
               :pos [(+ cx (* sx hx)) y]})
            (let [sy (Math/sign dy)]
              {:delta [0 (* py sy)]
               :normal [0 sy]
               :pos [x (+ cy (* sy hy))]})))))))

(defn test-aabb-intersect-point []
  (js/console.log (aabb-intersect-point [10 10 1 1] [5 3]))
  (js/console.log (aabb-intersect-point [10 10 1 1] [9 9]))
  (js/console.log (aabb-intersect-point [10 10 1 1] [10 10]))
  (js/console.log (aabb-intersect-point [10 10 1 1] [9.2 9.6])))



(defn intersect-segment [[bx by hx hy] [x y] [dx dy] [px py]]
  ;; when dx==0 or dy==0 we have problems

  (let [scale-x (/ 1.0 dx)
        scale-y (/ 1.0 dy)
        sign-x (Math/sign scale-x)
        sign-y (Math/sign scale-y)

        near-x (* scale-x
                  (- bx
                     (* sign-x (+ hx px))
                     x))
        near-y (* scale-y
                  (- by
                     (* sign-y (+ hy py))
                     y))
        far-x (* scale-x
                 (- (+ bx
                       (* sign-x (+ hx px)))
                    x))
        far-y (* scale-y
                 (- (+ by
                       (* sign-y (+ hy py)))
                    y))
        ]
    ;; not colliding at all, return nil
    (when-not (or (> near-x far-y) (> near-y far-x))
      (let [near (max near-x near-y)
            far (min far-x far-y)]
        (when near
          (js/console.log "near is" near-x near-y near)
          (when-not (or (>= near 1) (<= far 0))
            ;; collision
            (let [time (clamp near 0 1)
                  hdx (* near dx)
                  hdy (* near dy)]
              {:near near
               :far far
               :time time
               :normal (if (< near-y near-x)
                         [(- sign-x) 0]
                         [0 (- sign-y)])
               :delta [hdx hdy]
               :pos [(+ x hdx) (+ y hdy)]})))))))

(defn constrain-rect [rect old-pos new-pos]
  (let [hit (intersect-segment
             rect
             (vec2/as-vector old-pos)
             (vec2/as-vector (vec2/sub new-pos old-pos))
             [0 0])]
    (if hit
      ;; collided with box... reject
      (do
        (js/console.log "hit!" rect old-pos new-pos hit)
        (vec2/from-vector (:pos hit)))

      ;; no collision
      new-pos
      )
    )
  )

(defn constrain-rects [rects old-pos new-pos]
  (if (vec2/equals old-pos new-pos)
    new-pos
    (loop [[r & remain] rects
           new-pos new-pos]
      (if r
        (recur remain (constrain-rect r old-pos new-pos))
        new-pos))))


(defn circle-vector-intersection [[h k r] [x0 y0] [x1 y1]]
  (let [x1-x0 (- x1 x0)
        y1-y0 (- y1 y0)
        x0-h (- x0 h)
        y0-k (- y0 k)
        a (+ (* x1-x0 x1-x0) (* y1-y0 y1-y0))
        b (+ (* 2 x1-x0 x0-h) (* 2 y1-y0 y0-k))
        c (- (+ (* x0-h x0-h) (* y0-k y0-k))
             (* r r))
        desc (- (* b b) (* 4 a c))]
    ;(js/console.log a b c desc)
    (cond
      (neg? desc)
      ;; no answers
      [nil nil]

      (zero? desc)
      ;; one answer (glances circle)
      [(/ (- b) 2 a) nil]

      :default
      (let [sqr (Math/sqrt desc)
            t1 (/ (+ (- b) sqr) 2 a)
            t2 (/ (- (- b) sqr) 2 a)
            ]
        ;; two answers (through circle)
        [(min t1 t2) (max t1 t2)]))))

(defn both-inside?
  "is start and end both inside the circle?"
  [t1 t2]
  (and
   (neg? t1)
   (> t2 1)))

(defn outside->inside? [t1 t2]
  (and
   (< 0 t1 1)
   (> t2 1)))

(defn inside->outside? [t1 t2]
  (and
   (< t1 0)
   (< 0 t2 1)))

(defn outside->outside? [t1 t2]
  (and
   (< 0 t1 1)
   (< 0 t2 2)))

(defn circle-point-extract [[cx cy r] [x y]]
  (let [rx (- x cx)
        ry (- y cy)
        ex (+ x rx)
        ey (+ y ry)
        [t1 t2] (circle-vector-intersection [cx cy r] [x y] [ex ey])]
    (cond
      (< 0 t1) (vec2/lerp (vec2/vec2 x y) (vec2/vec2 ex ey) t1)
      (< 0 t2) (vec2/lerp (vec2/vec2 x y) (vec2/vec2 ex ey) t2)
      :default (vec2/vec2 x y))
    )
  )

(defn constrain-circle [circle old-pos new-pos]
  (if (vec2/equals old-pos new-pos)

    ;; not moving. extract from circle along shortest route
    (circle-point-extract circle (vec2/as-vector new-pos))

    ;; moving. collide with vector
    (let [[t1 t2] (circle-vector-intersection circle (vec2/as-vector old-pos) (vec2/as-vector new-pos))]
      (cond
        (nil? t1) new-pos
        (nil? t2) new-pos
        (both-inside? t1 t2) (circle-point-extract circle (vec2/as-vector old-pos))
        (outside->inside? t1 t2) (vec2/lerp old-pos new-pos t1)
        (inside->outside? t1 t2) new-pos
        (outside->outside? t1 t2) (vec2/lerp old-pos new-pos t1)
        :default new-pos))))

(defn test-constrain-rect []
  (js/console.log
   (constrain-rect [150 150 10 10] (vec2/vec2 152 140) (vec2/vec2 154 140))))

;(test-constrain-rect)
