(ns advent-2022-clojure.day08-test
  (:require [clojure.test :refer :all]
            [advent-2022-clojure.day08 :refer :all]))

(def test-data "30373\n25512\n65332\n33549\n35390")
(def puzzle-data (slurp "resources/day08-puzzle.txt"))

(deftest part1-test
  (are [expected input] (= expected (part1 input))
                        21 test-data
                        1789 puzzle-data))

(deftest part2-test
  (are [expected input] (= expected (part2 input))
                        8 test-data
                        314820 puzzle-data))

