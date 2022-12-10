(ns advent-2022-clojure.day10-test
  (:require [clojure.test :refer :all]
            [advent-2022-clojure.day10 :refer :all]))

(def test-data (slurp "resources/day10-test.txt"))
(def puzzle-data (slurp "resources/day10-puzzle.txt"))

(deftest part1-test
  (are [expected input] (= expected (part1 input))
                        13140 test-data
                        15140 puzzle-data))

; There is no test case for part 2 since it's graphical, but you can invoke the function to see what gets printed.
(part2 test-data)
(println)

; Part 2 spells out BPJAZGAP
(part2 puzzle-data)
;   xxx  xxx    xx  xx  xxxx  xx   xx  xxx x
;   x  x x  x    x x  x    x x  x x  x x  xx
;   xxx  x  x    x x  x   x  x    x  x x  x
;   x  x xxx     x xxxx  x   x xx xxxx xxx
;   x  x x    x  x x  x x    x  x x  x x
;   xxx  x     xx  x  x xxxx  xxx x  x x

