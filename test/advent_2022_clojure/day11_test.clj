(ns advent-2022-clojure.day11-test
  (:require [clojure.test :refer :all]
            [advent-2022-clojure.day11 :refer :all]))

(def test-data (slurp "resources/day11-test.txt"))
(def puzzle-data (slurp "resources/day11-puzzle.txt"))

(deftest part1-test
  (are [expected input] (= expected (part1 input))
                        10605 test-data
                        56350 puzzle-data))

(deftest part2-test
  (are [expected input] (= expected (part2 input))
                        2713310158 test-data
                        13954061248 puzzle-data))