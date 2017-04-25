(ns thekeep.state
  (:require [infinitelives.utils.vec2 :as vec2]))

(def scale 2)

(defonce state
  (atom
   {:pos (vec2/vec2 (* 5 16) (* 5 16))
    :health 100
    :score 0
    :sword nil
    :enemy (vec2/zero)
    :enemies []
    :running? false}))
