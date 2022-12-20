(ns advent-2022-clojure.day20
  (:require [clojure.string :as str]
            [advent-2022-clojure.utils :refer [index-of-first]]))

(defn parse-input [input]
  (vec (map-indexed #(vector %1 (parse-long %2)) (str/split-lines input))))

(defn index-of-original-index [idx nums]
  (index-of-first #(= (first %) idx) nums))

(defn rotate-at-index [idx nums]
  (let [[_ rotation :as node] (nums idx)
        idx' (mod (+ idx rotation) (dec (count nums)))]
    (cond (= idx idx') nums
          (< idx' idx) (reduce into (subvec nums 0 idx') [[node]
                                                          (subvec nums idx' idx)
                                                          (subvec nums (inc idx))])
          :else (reduce into (subvec nums 0 idx) [(subvec nums (inc idx) (inc idx'))
                                                  [node]
                                                  (subvec nums (inc idx'))]))))

(defn mix [nums]
  (reduce (fn [acc idx] (rotate-at-index (index-of-original-index idx acc) acc))
          nums
          (range 0 (count nums))))

(defn solve [decryption-key num-mixes input]
  (->> (parse-input input)
       (mapv #(update % 1 * decryption-key))
       (iterate mix)
       (drop num-mixes)
       first
       (map second)
       cycle
       (drop-while (complement zero?))
       (rest)
       (partition 1000)
       (map last)
       (take 3)
       (reduce +)))

(defn part1 [input] (solve 1 1 input))
(defn part2 [input] (solve 811589153 10 input))