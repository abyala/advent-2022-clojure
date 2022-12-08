(ns advent-2022-clojure.day08
  (:require [advent-2022-clojure.point :as p]
            [advent-2022-clojure.utils :refer [count-if char->int]]))

(defn parse-input [input]
  (reduce (fn [acc [k v]] (assoc acc k (char->int v)))
          {}
          (p/parse-to-char-coords input)))

(defn trees-in-direction [points p dir]
  (->> (iterate #(mapv + dir %) p)
       (map points)
       (next)
       (take-while some?)))

(defn visible?
  ([points pos] (some #(visible? points pos %) p/cardinal-directions))
  ([points pos dir]
   (let [p (points pos)]
     (every? #(< % p) (trees-in-direction points pos dir)))))

(defn viewing-distance
  ([points pos] (transduce (map #(viewing-distance points pos %)) * p/cardinal-directions))
  ([points pos dir]
   (let [p (points pos)]
     (reduce (fn [acc t] (if (>= t p) (reduced (inc acc)) (inc acc)))
             0
             (trees-in-direction points pos dir)))))

(defn part1 [input]
  (let [points (parse-input input)]
    (count-if #(visible? points %) (keys points))))

(defn part2 [input]
  (let [points (parse-input input)]
    (transduce (map (partial viewing-distance points)) max 0 (keys points))))
