(ns advent-2022-clojure.day20-test
  (:require [clojure.test :refer :all]
            [advent-2022-clojure.day20 :refer :all]))

(def test-data (slurp "resources/day20-test.txt"))
(def puzzle-data (slurp "resources/day20-puzzle.txt"))

(deftest rotate-at-index-test
  (let [v [[:a 1] [:b -2] [:c -2] [:d -4] [:e 2]]]
    (are [expected idx] (= expected (rotate-at-index idx v))
                        [[:b -2] [:a 1] [:c -2] [:d -4] [:e 2]] 0
                        [[:a 1] [:c -2] [:d -4] [:b -2] [:e 2]] 1
                        [[:c -2] [:a 1] [:b -2] [:d -4] [:e 2]] 2
                        v 3
                        [[:a 1] [:b -2] [:e 2] [:c -2] [:d -4]] 4)))


(deftest part1-test
    (are [expected input] (= expected (part1 input))
                          3 test-data
                          2827 puzzle-data))

(deftest part2-test
    (are [expected input] (= expected (part2 input))
                          1623178306 test-data
                          7834270093909 puzzle-data))