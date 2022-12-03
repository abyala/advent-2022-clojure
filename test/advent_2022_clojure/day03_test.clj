(ns advent-2022-clojure.day03-test
  (:require [clojure.test :refer :all]
            [advent-2022-clojure.day03 :refer :all]))

(def test-data "vJrwpWtwJgWrhcsFMMfFFhFp\njqHRNqRjqzjGDLGLrsFMfFZSrLrFZsSL\nPmmdzqPrVvPwwTWBwg\nwMqvLMZHhHMvwLHjbvcjnnSBnvTQFn\nttgJtRGJQctTZtZT\nCrZsJsPPZsGzwwsLwLmpwMDw")
(def puzzle-data (slurp "resources/day03-puzzle.txt"))

(deftest part1-test
  (is (= 157 (part1 test-data)))
  (is (= 7428 (part1 puzzle-data))))

(deftest part2-test
  (is (= 70 (part2 test-data)))
  (is (= 2650 (part2 puzzle-data))))