(ns stubbly.levenshtein)

(declare make-matrix distance-reducer print-matrix)

(defn distance
  "Calculates the levenshtein distance of two strings.
   Algorithm pilfered from wikipedia, and converted to clojure"
  [s t]
  (let [m (+ 1 (count s))
        n (+ 1 (count t))
        rows (make-matrix m n)
        reducer (partial distance-reducer s t)]
    (last (last
     (reduce reducer rows (for [j (range 1 m)
                                i (range 1 n)] [i j]))
      ))))

(defn- make-matrix
  "Makes a width*height zeroed out matrix, except the
   first row & column, which are both counting 0..width/height"
  [width height]
  (let [first-row (vec (for [j (range width)] j))
        other-rows (for [i (range 1 height)] (vec (concat [i] (repeat (- width 1) 0))))]
    (vec (concat [first-row] other-rows))))

(defn- distance-reducer
  [s t matrix [i j :as coord-pair]]
  (let [last-i (- i 1)
        last-j (- j 1)]
    (assoc-in matrix coord-pair
              (if (= (get t last-i) (get s last-j))
                (get-in matrix [last-i last-j])
                (->> [[last-i j] [i last-j] [last-i last-j]]
                     (map #(get-in matrix %))
                     (map inc)
                     (apply min))))))

(defn- print-matrix
  [matrix]
  (println "Matrix: ")
  (doseq [x matrix] (println x)))
