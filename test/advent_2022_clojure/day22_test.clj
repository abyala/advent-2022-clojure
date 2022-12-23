(ns advent-2022-clojure.day22-test
  (:require [clojure.test :refer :all]
            [advent-2022-clojure.day22 :refer :all]))

; ....
; .##.
; #..#
; ...#
(def move-test-data "....\n.##.\n#..#\n...#")
(def test-data (slurp "resources/day22-test.txt"))
(def puzzle-data (slurp "resources/day22-puzzle.txt"))

(deftest move-test
  (let [board (parse-board move-test-data)]
    (are [expected pos dir] (= {:pos expected :dir dir} (move board {:pos pos :dir dir} 1))
                            ; Top left corner
                            [2 1] [1 1] [1 0]
                            [4 1] [1 1] [-1 0]
                            [1 2] [1 1] [0 1]
                            [1 4] [1 1] [0 -1]

                            ; Second row - can't move right or down, can move up and wrap left
                            [1 2] [1 2] [1 0]
                            [4 2] [1 2] [-1 0]
                            [1 2] [1 2] [0 1]
                            [1 1] [1 2] [0 -1]

                            ; Fourth row - can't move up or wrap left, can move right and down
                            [2 4] [1 4] [1 0]
                            [1 4] [1 4] [-1 0]
                            [1 1] [1 4] [0 1]
                            [1 4] [1 4] [0 -1])))

(deftest turn-test
  (testing "Turning right"
    (are [expected input] (= {:pos [1 1] :dir expected} (turn {:pos [1 1] :dir input} :right))
                          [1 0] [0 -1]
                          [0 1] [1 0]
                          [-1 0] [0 1]
                          [0 -1] [-1 0]))
  (testing "Turning left"
    (are [expected input] (= {:pos [1 1] :dir expected} (turn {:pos [1 1] :dir input} :left))
                          [-1 0] [0 -1]
                          [0 -1] [1 0]
                          [1 0] [0 1]
                          [0 1] [-1 0])))

(deftest part1-test
  (are [expected input] (= expected (part1 input))
                        6032 test-data
                        60362 puzzle-data))

#_(deftest part2-test
  (are [expected input] (= expected (part2 input))
                        301 test-data
                        3617613952378 puzzle-data))