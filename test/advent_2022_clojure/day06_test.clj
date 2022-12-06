(ns advent-2022-clojure.day06-test
  (:require [clojure.test :refer :all]
            [advent-2022-clojure.day06 :refer :all]))

(def puzzle-data (slurp "resources/day06-puzzle.txt"))

(deftest part1-test
  (are [expected input] (= expected (part1 input))
                        7 "mjqjpqmgbljsphdztnvjfqwrcgsmlb"
                        5 "bvwbjplbgvbhsrlpgdmjqwftvncz"
                        6 "nppdvjthqldpwncqszvftbrmjlhg"
                        10 "nznrnfrfntjfmvfwmzdfjlvtqnbhcprsg"
                        11 "zcfzfwzzqfrljwzlrfnpqdbhtmscgvjw"
                        1262 puzzle-data))

(deftest part2-test
  (are [expected input] (= expected (part2 input))
                        19 "mjqjpqmgbljsphdztnvjfqwrcgsmlb"
                        23 "bvwbjplbgvbhsrlpgdmjqwftvncz"
                        23 "nppdvjthqldpwncqszvftbrmjlhg"
                        29 "nznrnfrfntjfmvfwmzdfjlvtqnbhcprsg"
                        26 "zcfzfwzzqfrljwzlrfnpqdbhtmscgvjw"
                        3444 puzzle-data))
