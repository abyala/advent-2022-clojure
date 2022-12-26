(ns advent-2022-clojure.day16-test
  (:require [clojure.test :refer :all]
            [advent-2022-clojure.day16 :refer :all]))

(def test-data (slurp "resources/day16-test.txt"))
(def puzzle-data (slurp "resources/day16-puzzle.txt"))

(deftest part1-test
  (are [expected input] (= expected (part1 input))
                        1651 test-data
                        1647 puzzle-data))

(deftest part2-test
  (are [expected input] (= expected (part1 input))
                        1707 test-data
                        2169 puzzle-data))