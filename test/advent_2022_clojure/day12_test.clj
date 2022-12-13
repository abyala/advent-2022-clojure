(ns advent-2022-clojure.day12-test
  (:require [clojure.test :refer :all]
            [advent-2022-clojure.day12 :refer :all]))

(def test-data (slurp "resources/day12-test.txt"))
(def puzzle-data (slurp "resources/day12-puzzle.txt"))

(deftest part1-test
  (are [expected input] (= expected (part1 input))
                        31 test-data
                        339 puzzle-data))

(deftest part2-test
    (are [expected input] (= expected (part2 input))
                          29 test-data
                          332 puzzle-data))