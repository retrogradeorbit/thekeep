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

   :rubble1 [240 112]
   :rubble2 [256 112]
   :rubble3 [272 112]
   :rubble4 [288 112]

   :walldec1 [0 112]
   :walldec2 [16 112]
   :walldec3 [32 112]
   :walldec4 [48 112]

   :portcullis [64 96]
   :door [64 112]
   })

(def hero-mapping
  {
   :up {:pos [320 112] :size [16 16]}
   :left {:pos [304 128] :size [16 16]}
   :right {:pos [336 128] :size [16 16]}
   :down {:pos [320 144] :size [16 16]}

   :sword {:pos [320 128] :size [16 16]}

   :lion {:pos [160 0] :size [32 48]}

   :fire {:pos [288 48] :size [32 48]}

   :enemy1 {:pos [336 112] :size [16 16]}
   :enemy2 {:pos [352 112] :size [16 16]}
   :enemy3 {:pos [368 112] :size [16 16]}

   :chest {:pos [128 112] :size [16 16]}

   :switch1 {:pos [0 80] :size [16 16]}

   :heart {:pos [304 144] :size [16 16]}
   })
