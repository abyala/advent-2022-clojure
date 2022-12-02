(ns advent-2022-clojure.day02-test
  (:require [clojure.test :refer :all]
            [advent-2022-clojure.day02 :refer :all]))

(def test-data "A Y\nB X\nC Z")
(def puzzle-data (slurp "resources/day02-puzzle.txt"))

(deftest part1-test
  (is (= 15 (part1 test-data)))
  (is (= 12276 (part1 puzzle-data))))

(deftest part2-test
  (is (= 12 (part2 test-data)))
  (is (= 9975 (part2 puzzle-data))))