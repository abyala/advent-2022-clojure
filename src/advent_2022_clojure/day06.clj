(ns advent-2022-clojure.day06)

(defn solve [n input]
  (->> (partition-all n 1 input)
       (keep-indexed (fn [idx letters] (when (= n (count (set letters)))
                                         (+ n idx))))
       first))

(defn part1 [input] (solve 4 input))
(defn part2 [input] (solve 14 input))