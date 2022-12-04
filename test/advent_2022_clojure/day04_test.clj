(ns advent-2022-clojure.day04-test
  (:require [clojure.test :refer :all]
            [advent-2022-clojure.day04 :refer :all]))

(def test-data "2-4,6-8\n2-3,4-5\n5-7,7-9\n2-8,3-7\n6-6,4-6\n2-6,4-8")
(def puzzle-data (slurp "resources/day04-puzzle.txt"))

(deftest part1-test
  (are [expected input] (= expected (part1 input))
                        2 test-data
                        450 puzzle-data))

(deftest part2-test
  (are [expected input] (= expected (part2 input))
                        4 test-data
                        837 puzzle-data))
