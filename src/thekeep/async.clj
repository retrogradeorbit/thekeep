(ns thekeep.async
  (:require [cljs.core.async.macros :as m]))

;; macros for more convenient game async
(defmacro <!* [test & body]
  `(let [result# (cljs.core.async/<! ~@body)]
     (if ~test
       result#
       (throw (js/Error "go-while exit")))))

(defmacro >!* [test & body]
  `(let [result# (cljs.core.async/>! ~@body)]
     (if ~test
       result#
       (throw (js/Error "go-while exit")))))

(comment
  (macroexpand-1 '(>!* @kill-atom (e/next-frame foo)))
  (macroexpand-1 '(<!* @kill-atom (e/next-frame foo))))

(defn process-body [test [head & tail]]
  (let [
        [processed-head processed-tail]
        (cond
          (identical? '() head) ['() tail]
          (identical? '[] head) ['[] tail]
          (list? head) [(process-body test head) tail]
          (vector? head) [(vec (process-body test head)) tail]
          (map? head) [(into {} (process-body test (seq head))) tail]
          (= '<! head) ['thekeep.async/<!* (cons test tail)]
          (= '>! head) ['thekeep.async/>!* (cons test tail)]
          :default [head tail])]
    (if processed-tail
      (cons processed-head (process-body test processed-tail))
      (list processed-head))))

(comment
  (process-body 'test2 '(
                         (<! (r/load-resources canvas :ui ["img/tiles.png"
                                                           "img/notlink.png"]))
                         (t/load-sprite-sheet!
                          (r/get-texture :notlink :nearest)
                          assets/hero)

                         (let [tiles (tm/make-tile-set :tiles assets/tile-set-mapping [16 16])
                               player (s/make-sprite :down-1 :scale scale :x 0 :y 0)
                               walk-to-chan (chan)
                               level (tilemap/make-tile-maps
                                      tiles
                                      (-> world/rooms :corner-room :map))
                               ]
                           (m/with-sprite :tilemap
                             [container (s/make-container
                                         :children [player]
                                         :scale 3)]
                             (loop []
                               (<! (e/next-frame))
                               (recur)))))))

(comment
  (process-body 'test2 '(do (foo) (bar) (>! (nf))
                            (foo [])
                            ()
                            {}
                            (loop [t (<! (nf))
                                   p {:a (<! b)
                                      :b :b
                                      (<! c) :c}] (bing) (<! (nf))
                                      (when bong (recur (inc t))))))

  (clojure.core/destructure '[])

  (process-body 'test '(foo [] ())))

(defmacro go-while [test & body]
  `(m/go ~@(process-body test body)))

(defmacro continue-while [test & body]
  `(do m/go ~@(process-body test body)))

(defmacro go-until-reload [state & body]
  `(let [counter# (:__figwheel_counter @~state)]
     (go-while (= counter# (:__figwheel_counter @~state))
               ~@body))
)

#_
(macroexpand-1
 '(go-while true
            (<! (r/load-resources canvas :ui ["img/tiles.png"
                                              "img/notlink.png"]))
            (t/load-sprite-sheet!
             (r/get-texture :notlink :nearest)
             assets/hero)

            (let [tiles (tm/make-tile-set :tiles assets/tile-set-mapping [16 16])
                  player (s/make-sprite :down-1 :scale scale :x 0 :y 0)
                  walk-to-chan (chan)
                  level (tilemap/make-tile-maps
                         tiles
                         (-> world/rooms :corner-room :map))
                  ]
              (m/with-sprite :tilemap
                [container (s/make-container
                            :children [player]
                            :scale 3)]
                (loop []
                  (<! (e/next-frame))
                  (recur))))))

#_
(macroexpand-1 '(fortress.async/<!* true (e/next-frame)))


(comment
  (macroexpand-1
   '(go-while (= a b) (do (foo) (bar) (<! (nf))
                          (loop [] (bing) (<! (nf)) (when bong (recur)))
                          last)
              very-last
              )))
