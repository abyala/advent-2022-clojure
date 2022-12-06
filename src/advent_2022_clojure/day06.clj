(ns advent-2022-clojure.day06
  (:require [advent-2022-clojure.utils :refer [index-of-first]]))

(defn solve [n input]
  (->> (partition-all n 1 input)
       (index-of-first #(= n (count (set %))))
       (+ n)))

(defn part1 [input] (solve 4 input))
(defn part2 [input] (solve 14 input))