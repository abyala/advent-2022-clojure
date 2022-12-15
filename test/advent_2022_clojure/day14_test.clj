(ns advent-2022-clojure.day14-test
  (:require [clojure.test :refer :all]
            [advent-2022-clojure.day14 :refer :all]))

(def test-data (slurp "resources/day14-test.txt"))
(def puzzle-data (slurp "resources/day14-puzzle.txt"))

(deftest part1-test
  (are [expected input] (= expected (part1 input))
                        24 test-data
                        1001 puzzle-data))

(deftest part2-test
    (are [expected input] (= expected (part2 input))
                          93 test-data
                          27976 puzzle-data))


