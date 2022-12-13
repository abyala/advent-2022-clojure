(ns advent-2022-clojure.day13
  (:require
    [clojure.edn :as edn]
    [clojure.string :as str]
    [advent-2022-clojure.utils :as u]))

(def divider-packets #{[[2]] [[6]]})

(defn move-up [address]
  (subvec address 0 (dec (count address))))
(defn move-down [address]
  (conj address 0))
(defn move-right [address]
  (update address (dec (count address)) inc))
(defn vectorize [packet address]
  (update-in packet address vector))

(defn correct-order?
  ([left right] (correct-order? left right [0]))
  ([left right address]
   (let [[v1 v2 :as values] (map #(get-in % address) [left right])]
     (cond
       (every? nil? values) (recur left right (-> address move-up move-right))
       (nil? v1) true
       (nil? v2) false
       (every? number? values) (case (u/signum (- v1 v2))
                                 -1 true
                                 1 false
                                 (recur left right (move-right address)))
       (every? vector? values) (recur left right (move-down address))
       (vector? v1) (recur left (vectorize right address) address)
       :else (recur (vectorize left address) right address)))))

(defn part1 [input]
  (transduce (keep-indexed #(when (apply correct-order? %2) (inc %1)))
             +
             (u/split-blank-line-groups edn/read-string input)))

(defn sort-packets [packets]
  (sort (fn [a b] (if (correct-order? a b) -1 1)) packets))

(defn part2 [input]
  (transduce (keep-indexed (fn [idx v] (when (divider-packets v) (inc idx))))
             *
             (->> (str/split-lines input)
                  (remove str/blank?)
                  (map edn/read-string)
                  (concat divider-packets)
                  sort-packets)))
