(ns advent-2022-clojure.day01-test
  (:require [clojure.test :refer :all]
            [advent-2022-clojure.day01 :refer :all]))

(def test-data "1000\n2000\n3000\n\n4000\n\n5000\n6000\n\n7000\n8000\n9000\n\n10000")
(def puzzle-data (slurp "resources/day01-puzzle.txt"))

(deftest part1-test
  (are [expected input] (= expected (part1 input))
                        24000 test-data
                        70698 puzzle-data))

(deftest part2-test
  (are [expected input] (= expected (part2 input))
                        45000 test-data
                        206643 puzzle-data))