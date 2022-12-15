(ns advent-2022-clojure.day15-test
  (:require [clojure.test :refer :all]
            [advent-2022-clojure.day15 :refer :all]))

(def test-data (slurp "resources/day15-test.txt"))
(def puzzle-data (slurp "resources/day15-puzzle.txt"))

(deftest part1-test
  (are [expected row input] (= expected (part1 row input))
                            26 10 test-data
                            5870800 2000000 puzzle-data))

(deftest part2-test
  (are [expected max-xy input] (= expected (part2 max-xy input))
                               56000011 20 test-data
                               10908230916597 4000000 puzzle-data))


