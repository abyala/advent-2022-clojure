(ns advent-2022-clojure.utils-test
  (:require [clojure.test :refer :all]
            [advent-2022-clojure.utils :refer :all]))

(deftest count-if-test
  (are [expected f input] (= expected (count-if f input))
                          0 even? ()
                          0 even? []
                          0 even? [1 3 5]
                          2 even? [1 2 3 4 5]))

(deftest index-of-first-test
  (testing "Tests that should return nil"
    (are [f input] (nil? (index-of-first f input))
                   even? ()
                   even? []
                   even? [1 3 5]
                   some? ""
                   first []))
  (testing "Tests that return indexes"
    (are [expected f input] (= expected (index-of-first f input))
                            1 even? [1 2 3]
                            0 even? (range)
                            0 some? "abc"
                            2 #{2 3 4} (range))))

