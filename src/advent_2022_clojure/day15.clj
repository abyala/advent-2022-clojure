(ns advent-2022-clojure.day15
  (:require [clojure.string :as str]
            [advent-2022-clojure.point :as p]))

(defn parse-line [s]
  (let [[x1 y1 x2 y2] (map parse-long (re-seq #"-?\d+" s))]
    {:sensor [x1 y1] :beacon [x2 y2]}))

(defn parse-input [input]
  (->> input str/split-lines (map parse-line)))

(defn blocked-ranges-for-row [target-row readings]
  (keep (fn [{:keys [sensor beacon]}]
          (let [[sensor-x sensor-y] sensor
                total-distance (p/manhattan-distance sensor beacon)
                x-distance (- total-distance (abs (- target-row sensor-y)))]
            (when-not (neg-int? x-distance)
              [(- sensor-x x-distance) (+ sensor-x x-distance)])))
        readings))

(defn combine-blocked-ranges [ranges]
  (reduce (fn [acc [low' high' :as r]]
            (let [[low high] (last acc)]
              (cond
                (nil? low) [r]
                (<= low' (inc high)) (update-in acc [(dec (count acc)) 1] max high')
                :else (conj acc r))))
          []
          (sort ranges)))

(defn num-beacons-for-row [target-row readings]
  (->> readings (map :beacon) (filter #(= target-row (second %))) distinct count))

(defn part1 [row input]
  (let [readings (parse-input input)
        ranges (combine-blocked-ranges (blocked-ranges-for-row row readings))]
    (- (transduce (map (comp inc abs (partial apply -))) + ranges)
       (num-beacons-for-row row readings))))

(defn part2 [max-xy input]
  (let [readings (parse-input input)]
    (first (for [y (range 0 (inc max-xy))
                 :let [ranges (combine-blocked-ranges (blocked-ranges-for-row y readings))]
                 :when (= 2 (count ranges))
                 :let [x (-> ranges first second inc)]
                 :when (<= 0 x max-xy)]
             (+ (* x 4000000) y)))))