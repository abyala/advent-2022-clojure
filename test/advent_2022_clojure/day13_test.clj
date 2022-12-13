(ns advent-2022-clojure.day13-test
  (:require [clojure.test :refer :all]
            [advent-2022-clojure.day13 :refer :all]))

(def test-data (slurp "resources/day13-test.txt"))
(def puzzle-data (slurp "resources/day13-puzzle.txt"))

(deftest part1-test
  (are [expected input] (= expected (part1 input))
                          13 test-data
                          5393 puzzle-data))

(deftest part2-test
    (are [expected input] (= expected (part2 input))
                         140 test-data
                         26712 puzzle-data))

