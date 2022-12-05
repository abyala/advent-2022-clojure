(ns advent-2022-clojure.day05-test
  (:require [clojure.test :refer :all]
            [advent-2022-clojure.day05 :refer :all]))

(def test-data "")
(def puzzle-data (slurp "resources/day05-puzzle.txt"))

(deftest part1-test
  (is (= "SPFMVDTZT" (part1 puzzle-data))))

(deftest part2-test
  (is (= "ZFSJBPRFP" (part2 puzzle-data))))
