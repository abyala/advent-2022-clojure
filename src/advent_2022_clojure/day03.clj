(ns advent-2022-clojure.day03
  (:require
    [clojure.set :as set]
    [clojure.string :as str]))

(defn string-halves [s]
  (split-at (/ (count s) 2) s))

(defn find-duplicate [strings]
  (->> (map set strings)
       (apply set/intersection)
       first))

(defn priority [^Character c]
  (- (int c) (if (Character/isUpperCase c) 38 96)))

(defn solve [grouping-fn input]
  (transduce (map (comp priority find-duplicate)) + (grouping-fn (str/split-lines input))))

(defn part1 [input] (solve (partial map string-halves) input))
(defn part2 [input] (solve (partial partition 3) input))
