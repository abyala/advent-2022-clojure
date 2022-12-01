(ns advent-2022-clojure.day01
  (:require [advent-2022-clojure.utils :as utils]
            [clojure.string :as str]))

(defn parse-calories [s]
  (transduce (map parse-long) + (str/split-lines s)))

(defn solve [n input]
  (->> (utils/split-blank-line input)
       (map parse-calories)
       (sort >)
       (take n)
       (reduce +)))

(defn part1 [input] (solve 1 input))
(defn part2 [input] (solve 3 input))
