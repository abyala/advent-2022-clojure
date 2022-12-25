(ns advent-2022-clojure.day24-test
  (:require [clojure.test :refer :all]
            [advent-2022-clojure.day24 :refer :all]))

(def test-complex-data (slurp "resources/day24-test-complex.txt"))
(def puzzle-data (slurp "resources/day24-puzzle.txt"))

(deftest part1-test
  (are [expected input] (= expected (part1 input))
                        18 test-complex-data
                        326 puzzle-data))

(deftest part2-test
  (are [expected input] (= expected (part2 input))
                        54 test-complex-data
                        976 puzzle-data))