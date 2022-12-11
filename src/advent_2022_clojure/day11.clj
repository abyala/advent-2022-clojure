(ns advent-2022-clojure.day11
  (:require [clojure.string :as str]
            [advent-2022-clojure.utils :as u :refer [divisible?]]))

(defn parse-single-number [s]
  (parse-long (first (re-seq #"\d+" s))))

(defn parse-monkey [s]
  (let [[monkey-line starting-line op-line test-line true-line false-line] (str/split-lines s)]
    {:id           (parse-single-number monkey-line)
     :items        (mapv parse-long (re-seq #"\d+" starting-line))
     :worry-raiser (let [[_ op-str op-arg] (first (re-seq #".*old (\W) (.*)" op-line))
                         op-fn ({"*" * "+" +} op-str)]
                     (if (= "old" op-arg) (fn [v] (op-fn v v))
                                          (fn [v] (op-fn v (parse-long op-arg)))))
     :test-divisor (parse-single-number test-line)
     :true-monkey  (parse-single-number true-line)
     :false-monkey (parse-single-number false-line)
     :inspections  0}))

(defn parse-monkeys [input]
  (mapv parse-monkey (u/split-by-blank-lines input)))

(defn next-monkey [test-divisor true-monkey false-monkey item]
  (if (divisible? item test-divisor) true-monkey false-monkey))

(defn create-worry-reducer [reduce-worry? monkeys]
  (if reduce-worry?
    (fn [item] (quot item 3))
    (let [crt-product (reduce * (map :test-divisor monkeys))]
      (fn [item] (rem item crt-product)))))

(defn process-monkey [worry-reducer monkeys monkey-id]
  (let [{:keys [items worry-raiser test-divisor true-monkey false-monkey]} (monkeys monkey-id)]
    (reduce (fn [monkeys' item]
              (let [item' (-> item worry-raiser worry-reducer)
                    target (next-monkey test-divisor true-monkey false-monkey item')]
                (update-in monkeys' [target :items] conj item')))
            (-> monkeys
                (assoc-in [monkey-id :items] [])
                (update-in [monkey-id :inspections] + (count items)))
            items)))

(defn process-monkeys [worry-reducer monkeys]
  (reduce (partial process-monkey worry-reducer) monkeys (range (count monkeys))))

(defn solve [reduce-worry? num-iterations input]
  (let [monkeys (parse-monkeys input)
        worry-reducer (create-worry-reducer reduce-worry? monkeys)]
    (->> (iterate (partial process-monkeys worry-reducer) monkeys)
         (drop num-iterations)
         first
         (map :inspections)
         (sort >)
         (take 2)
         (apply *))))

(defn part1 [input] (solve true 20 input))
(defn part2 [input] (solve false 10000 input))
