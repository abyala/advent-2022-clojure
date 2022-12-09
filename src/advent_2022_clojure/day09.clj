(ns advent-2022-clojure.day09
  (:require [clojure.string :as str]
            [advent-2022-clojure.point :as p]))

(defn create-rope [n] (vec (repeat n p/origin)))

(defn parse-line [line]
  (let [[dir amt] (str/split line #" ")]
    (repeat (parse-long amt)
            ({"L" p/left "R" p/right "U" p/up "D" p/down} dir))))

(defn parse-input [input]
  (mapcat parse-line (str/split-lines input)))

(defn move-head [state dir]
  (update state 0 (partial mapv + dir)))

(defn move-ordinate [head-ord tail-ord]
  (condp apply [head-ord tail-ord] = tail-ord
                                   < (dec tail-ord)
                                   > (inc tail-ord)))

(defn pull-rope
  ([state]
   (reduce pull-rope state (range 1 (count state))))

  ([state knot-id]
   (let [[head tail] (map state [(dec knot-id) knot-id])]
     (if (p/touching? head tail)
       state
       (update state knot-id (partial mapv move-ordinate head))))))

(defn move [state dir]
  (-> state (move-head dir) pull-rope))

(defn solve [knots input]
  (->> (parse-input input)
       (reductions move (create-rope knots))
       (map last)
       set
       count))

(defn part1 [input] (solve 2 input))
(defn part2 [input] (solve 10 input))
