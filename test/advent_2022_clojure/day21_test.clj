(ns advent-2022-clojure.day21-test
  (:require [clojure.test :refer :all]
            [advent-2022-clojure.day21 :refer :all]))

(def test-data (slurp "resources/day21-test.txt"))
(def puzzle-data (slurp "resources/day21-puzzle.txt"))

(deftest part1-test
  (are [expected input] (= expected (part1 input))
                        152 test-data
                        80326079210554 puzzle-data))

(deftest part2-test
  (are [expected input] (= expected (part2 input))
                        301 test-data
                        3617613952378 puzzle-data))