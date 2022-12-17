(ns advent-2022-clojure.day16-test
  (:require [clojure.test :refer :all]
            [advent-2022-clojure.day16 :refer :all]))

(def test-data (slurp "resources/day16-puzzle.txt"))
(def puzzle-data (slurp "resources/day16-puzzle.txt"))

(deftest part1-test
  (is (= 1651 (part1 test-data)))
  (is (= 1647 (part1 puzzle-data))))

#_(deftest part2-test
  (is (= 12 (part2 test-data)))
  (is (= 9975 (part2 puzzle-data))))