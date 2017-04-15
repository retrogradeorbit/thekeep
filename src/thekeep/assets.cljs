(ns thekeep.assets)

;; 16 x 16

(def tile-mapping
  {
   :floor [96 32]

   ;; outside walls
   :tl1 [80 0]
   :tl2 [80 16]
   :t1 [96 0]
   :t2 [96 16]
   :tr1 [112 0]
   :tr2 [112 16]

   :l [80 32]
   :r [112 32]

   :bl [80 48]
   :b [96 48]
   :br [112 48]

   ;; inside walls
   :itl1 [128 0]
   :itl2 [128 16]
   :itr1 [144 0]
   :itr2 [144 16]

   :ibl1 [128 32]
   :ibl2 [128 48]
   :ibr1 [144 32]
   :ibr2 [144 48]

   :specr [48 32]
   :specl [64 32]

   })
