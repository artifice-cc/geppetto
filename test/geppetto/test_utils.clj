(ns geppetto.test-utils)

(defn nearly=
  ([a b epsilon]
     (<= (Math/abs (- a b)) epsilon))
  ([a b] (nearly= a b 0.01)))
