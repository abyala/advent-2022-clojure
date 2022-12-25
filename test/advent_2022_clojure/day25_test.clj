(ns advent-2022-clojure.day25-test
  (:require [clojure.test :refer :all]
            [advent-2022-clojure.day25 :refer :all]))

(def test-data (slurp "resources/day25-test.txt"))
(def puzzle-data (slurp "resources/day25-puzzle.txt"))

(deftest part1-test
  (are [expected input] (= expected (part1 input))
                        "2=-1=0" test-data
                        "2-02===-21---2002==0" puzzle-data))

