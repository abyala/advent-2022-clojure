(ns advent-2022-clojure.day04
  (:require [clojure.string :as str]
            [advent-2022-clojure.utils :as u]))

(defn parse-line [line]
  (map parse-long (re-seq #"\d+" line)))

(defn fully-contains? [[a b c d]]
  (or (<= a c d b) (<= c a b d)))

(defn overlaps? [[a b c d]]
  (or (<= a c b) (<= c a d)))

(defn solve [pred input]
  (->> (str/split-lines input)
       (map parse-line)
       (u/count-if pred)))

(defn part1 [input] (solve fully-contains? input))
(defn part2 [input] (solve overlaps? input))
