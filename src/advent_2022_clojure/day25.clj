(ns advent-2022-clojure.day25
  (:require [clojure.string :as str]
            [clojure.set :as set]))

(def powers-of-five (map #(Math/pow 5 %) (range)))
(def snafu-charmap {\2 2, \1 1, \0 0, \- -1, \= -2})

(defn snafu->decimal [snafu]
  (reduce + (map * (map snafu-charmap (reverse snafu)) powers-of-five)))

(def min-snafus-by-digit
  (letfn [(next-min [acc [x & xs]] (lazy-seq (cons (- x acc)
                                                   (next-min (+ acc x x) xs))))]
    (next-min 0 powers-of-five)))

(defn num-digits-in-snafu [n]
  (count (take-while (partial >= n) min-snafus-by-digit)))

(defn num-snafus [five n]
  (->> (range 2 -3 -1)
       (map (fn [multiple] [(abs (- n (* multiple five))) multiple]))
       (sort-by first)
       first
       second))

(defn decimal->snafu [n]
  (let [charmap (set/map-invert snafu-charmap)]
    (first (reduce (fn [[result remaining] five] (let [digit-nums (num-snafus five remaining)]
                                                 [(str result (charmap digit-nums))
                                                  (- remaining (* digit-nums five))]))
                 ["" n]
                 (reverse (take (num-digits-in-snafu n) powers-of-five))))))

(defn part1 [input]
  (decimal->snafu (transduce (map snafu->decimal) + (str/split-lines input))))
