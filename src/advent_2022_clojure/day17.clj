(ns advent-2022-clojure.day17
  (:require [clojure.string :as str]
            [clojure.set :as set]
            [advent-2022-clojure.point :as p]
            [advent-2022-clojure.utils :as u]))

(def tunnel-width 7)

(def shapes [[[0 0] [1 0] [2 0] [3 0]]
             [[1 0] [0 1] [1 1] [2 1] [1 2]]
             [[0 0] [1 0] [2 0] [2 1] [2 2]]
             [[0 0] [0 1] [0 2] [0 3]]
             [[0 0] [1 0] [0 1] [1 1]]])

(defn fits-horizontally? [shape]
  (every? #(< -1 % tunnel-width) (map first shape)))

(defn clear-of-rocks? [rocks shape]
  (not-any? rocks shape))

(defn clear? [rocks shape]
  (and (fits-horizontally? shape)
       (clear-of-rocks? rocks shape)))

(defn resting? [rocks shape]
  (let [fallen-shape (map (fn [[x y]] [x (dec y)]) shape)]
    (or (some neg-int? (map second fallen-shape))
        (some rocks fallen-shape))))

(defn starting-pos [rocks]
  [2 (+ 4 (transduce (map second) max -1 rocks))])

(defn push-shape [rocks shape dir]
  (let [points' (map (partial mapv + dir) shape)]
    (if (clear? rocks points') points' shape)))

(defn drop-shape [rocks shape]
  (if (resting? rocks shape) shape (map (fn [[x y]] [x (dec y)]) shape)))

(defn add-shape [rocks shape wind]
  (loop [points (map (partial mapv + (starting-pos rocks)) shape)
         wind wind
         num-steps 1]
    (let [pushed (push-shape rocks points (first wind))
          dropped (drop-shape rocks pushed)]
      (if (= pushed dropped)
        [pushed num-steps]
        (recur dropped (rest wind) (inc num-steps))))))

(defn drop-redundant-rocks [rocks]
  (let [min-max (->> (group-by first rocks)
                     (map second)
                     (map (partial transduce (map second) max 0))
                     (reduce min)
                     (+ -3))]
    (reduce (fn [acc [_ y :as p]] (if (< y min-max)
                                    acc
                                    (conj acc p)))
            #{}
            rocks)))

(defn rock-seq
  ([wind] (rock-seq #{} (cycle shapes) (cycle (map {\> [1 0] \< [-1 0]} wind))))
  ([rocks shapes wind]
   (let [[dropped-shape num-moves] (add-shape rocks (first shapes) wind)
         rocks' (drop-redundant-rocks (apply conj rocks dropped-shape))]
     (lazy-seq (cons rocks'
                     (rock-seq rocks' (rest shapes) (drop num-moves wind)))))))

(defn highest-rocks [rocks]
  (->> (group-by first rocks)
       (map (fn [[x entries]] [x (reduce max -1 (map second entries))]))
       (into {0 -1, 1 -1, 2 -1, 3 -1, 4 -1, 5 -1, 6 -1})))

(defn part1 [wind]
  (time (->> (rock-seq wind)
             (drop 2021)
             first
             (map second)
             (reduce max)
             inc)))

; This will never complete
(defn part2 [wind]
  (time (->> (rock-seq wind)
             (drop 1000000000000)
             first
             (map second)
             (reduce max)
             inc)))