(ns advent-2022-clojure.day07-test
  (:require [clojure.test :refer :all]
            [advent-2022-clojure.day07 :refer :all]))

(def test-data "$ cd /\n$ ls\ndir a\n14848514 b.txt\n8504156 c.dat\ndir d\n$ cd a\n$ ls\ndir e\n29116 f\n2557 g\n62596 h.lst\n$ cd e\n$ ls\n584 i\n$ cd ..\n$ cd ..\n$ cd d\n$ ls\n4060174 j\n8033020 d.log\n5626152 d.ext\n7214296 k")
(def puzzle-data (slurp "resources/day07-puzzle.txt"))

(deftest part1-test
  (are [expected input] (= expected (part1 input))
                        95437 test-data
                        2104783 puzzle-data))

(deftest part2-test
  (are [expected input] (= expected (part2 input))
                        24933642 test-data
                        5883165 puzzle-data))
