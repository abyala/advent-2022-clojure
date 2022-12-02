(ns advent-2022-clojure.day01
  (:require [advent-2022-clojure.utils :as utils]))

(defn solve [n input]
  (->> (utils/split-blank-line-groups parse-long input)
       (map (partial reduce +))
       (sort >)
       (take n)
       (reduce +)))

(defn part1 [input] (solve 1 input))
(defn part2 [input] (solve 3 input))
