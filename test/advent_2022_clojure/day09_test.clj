(ns advent-2022-clojure.day09-test
  (:require [clojure.test :refer :all]
            [advent-2022-clojure.day09 :refer :all]))

(def test-data "R 4\nU 4\nL 3\nD 1\nR 4\nD 1\nL 5\nR 2")
(def longer-test-data "R 5\nU 8\nL 8\nD 3\nR 17\nD 10\nL 25\nU 20")
(def puzzle-data (slurp "resources/day09-puzzle.txt"))

(deftest part1-test
  (are [expected input] (= expected (part1 input))
                        13 test-data
                        5883 puzzle-data))

(deftest part2-test
  (are [expected input] (= expected (part2 input))
                        1 test-data
                        36 longer-test-data
                        2367 puzzle-data))

