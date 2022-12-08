(ns advent-2022-clojure.point
  (:require [clojure.string :as str]))

(def origin [0 0])
(def left [-1 0])
(def right [1 0])
(def up [0 -1])
(def down [0 1])
(def cardinal-directions [left right up down])

(defn parse-to-char-coords
  "Given an input string of a multi-line grid of single characters, returns a lazy sequence of [[x y] c] tuples of
  [x y] coords to each character c. If the function f is provided, it transforms each value c using that function."
  ([input] (parse-to-char-coords identity input))
  ([f input] (->> (str/split-lines input)
                  (map-indexed (fn [y line]
                                 (map-indexed (fn [x c] [[x y] (f c)]) line)))
                  (apply concat))))

(defn parse-to-char-coords-map
  "Given an input string of a multi-line grid of single characters, returns a map of {[x y] c} mapping the [x y]
  coordinates to each character c. If the function f is provided, it transforms each value c using that function."
  ([input] (parse-to-char-coords-map identity input))
  ([f input] (into {} (parse-to-char-coords f input))))

(defn inclusive-distance [[x1 y1] [x2 y2]]
  (letfn [(local-dist [^long v1 ^long v2] (Math/abs (- v1 v2)))]
    (inc (max (local-dist x1 x2)
              (local-dist y1 y2)))))

(defn infinite-points-from [[x1 y1] [x2 y2]]
  (letfn [(ordinate-fn [v1 v2] (cond (< v1 v2) inc
                                     (> v1 v2) dec
                                     :else identity))]
    (let [x-fn (ordinate-fn x1 x2)
          y-fn (ordinate-fn y1 y2)]
      (map vector (iterate x-fn x1) (iterate y-fn y1)))))

(defn inclusive-line-between
  ([[point1 point2]]
   (inclusive-line-between point1 point2))

  ([point1 point2]
   (take (inclusive-distance point1 point2) (infinite-points-from point1 point2))))

(defn horizontal-line?
  ([[point1 point2]] (horizontal-line? point1 point2))
  ([[_ y1] [_ y2]] (= y1 y2)))

(defn vertical-line?
  ([[point1 point2]] (vertical-line? point1 point2))
  ([[x1 _] [x2 _]] (= x1 x2)))

(defn neighbors [point]
  (map (partial mapv + point) [[0 1] [0 -1] [-1 0] [1 0]]))

(defn surrounding
  ([point] (surrounding false point))
  ([include-self? point] (let [points (if include-self? [[-1 -1] [0 -1] [1 -1] [-1 0] [0 0] [1 0] [-1 1] [0 1] [1 1]]
                                                        [[-1 -1] [-1 0] [-1 1] [0 -1] [0 1] [1 -1] [1 0] [1 1]])]
                           (map (partial mapv + point) points))))

(defn perimeter-points [[x0 y0] [x1 y1]]
  (concat (for [x [x0 x1], y (range y0 (inc y1))] [x y])
          (for [y [y0 y1], x (range (inc x0) x1)] [x y])))