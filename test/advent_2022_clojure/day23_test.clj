(ns advent-2022-clojure.day23-test
  (:require [clojure.test :refer :all]
            [advent-2022-clojure.day23 :refer :all]))

(def test-simple-data (slurp "resources/day23-test-simple.txt"))
(def test-complex-data (slurp "resources/day23-test-complex.txt"))
(def puzzle-data (slurp "resources/day23-puzzle.txt"))

(deftest elves-seq-test
  (are [input expected] (every? true? (map = expected (elves-seq (parse-elves input))))
                        test-simple-data
                        [#{[2 1] [3 1] [2 2] [2 4] [3 4]}
                         #{[2 0] [3 0] [2 2] [3 3] [2 4]}
                         #{[2 1] [3 1] [1 2] [4 3] [2 5]}
                         #{[2 0] [4 1] [0 2] [4 3] [2 5]}]

                        test-complex-data
                        [#{[7 2] [5 3] [6 3] [7 3] [9 3] [3 4] [7 4] [9 4]
                           [4 5] [8 5] [9 5] [3 6] [5 6] [6 6] [7 6]
                           [3 7] [4 7] [6 7] [8 7] [9 7] [4 8] [7 8]}
                         #{[7 1] [5 2] [9 2] [3 3] [6 3] [8 3] [7 4] [10 4]
                           [4 5] [6 5] [8 5] [9 5] [2 6] [5 6] [7 6] [2 7] [4 7] [6 7] [8 7] [9 7]
                           [4 9] [7 9]}
                         #{[7 1] [4 2] [10 2] [3 3] [6 3] [8 3] [7 4] [11 4]
                           [3 5] [6 5] [8 5] [1 6] [5 6] [7 6] [9 6]
                           [2 8] [4 8] [6 8] [8 8] [9 8] [4 9] [7 9]}
                         #{[7 1] [5 2] [10 2] [2 3] [5 3] [9 3] [7 4] [11 4]
                           [3 5] [6 5] [8 5] [1 6] [4 6] [10 6] [7 7] [8 7]
                           [2 8] [3 8] [5 8] [10 8] [3 9] [7 10]}
                         #{[7 1] [6 2] [11 2] [2 3] [6 3] [7 3] [3 4] [9 4] [11 4]
                           [9 5] [1 6] [5 6] [6 6] [7 6] [10 6] [2 7] [9 7]
                           [4 8] [5 8] [10 8] [4 9] [7 10]}
                         #{[7 0] [2 2] [5 2] [11 2] [9 3] [6 4] [7 4] [11 4]
                           [1 5] [3 5] [5 5] [6 5] [7 5] [8 5] [11 6]
                           [4 7] [5 7] [8 7] [2 8] [10 9] [4 10] [7 10]}])

  (is (= #{[7 0] [11 1] [2 2] [4 2] [7 2] [6 3] [3 4] [9 4] [12 4] [1 5] [8 5] [9 5]
           [5 6] [6 6] [2 7] [11 7] [4 8] [6 8] [9 8] [4 10] [7 10] [10 10]}
         (nth (elves-seq (parse-elves test-complex-data)) 10))))

(deftest part1-test
  (are [expected input] (= expected (part1 input))
                        110 test-complex-data
                        3923 puzzle-data))

(deftest part2-test
  (are [expected input] (= expected (part2 input))
                        20 test-complex-data
                        1019 puzzle-data))