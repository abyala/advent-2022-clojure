(ns advent-2022-clojure.day17-test
  (:require [clojure.test :refer :all]
            [advent-2022-clojure.day17 :refer :all]))

(def test-data (slurp "resources/day17-test.txt"))
(def puzzle-data (slurp "resources/day17-puzzle.txt"))

(deftest part1-test
  (are [expected input] (= expected (part1 input))
                            3068 test-data
                            3149 puzzle-data))

#_(deftest part2-test
  (are [expected max-xy input] (= expected (part2 max-xy input))
                               56000011 20 test-data
                               10908230916597 4000000 puzzle-data))


