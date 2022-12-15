(ns advent-2022-clojure.day14
  (:require
    [clojure.string :as str]
    [clojure.set :as set]
    [advent-2022-clojure.utils :refer [signum]]))

(def cave-entrance [500 0])

(defn parse-point [s] (mapv parse-long (str/split s #",")))

(defn line-of-points [[x1 y1 :as p1] [x2 y2 :as p2]]
  (case (map (comp signum -) p1 p2)
    [0 0] [p1]
    [0 1] (map vector (repeat x1) (range y2 (inc y1)))
    [1 0] (map vector (range x2 (inc x1)) (repeat y1))
    (recur p2 p1)))

(defn rock-points [s]
  (->> (re-seq #"\d+,\d+" s)
       (map parse-point)
       (partition 2 1)
       (mapcat (partial apply line-of-points))
       set))

(defn parse-cave [input]
  (transduce (map rock-points) set/union #{} (str/split-lines input)))

(defn cave-floor [points]
  (->> points (map second) (reduce max) inc))

(defn possible-next-spaces [[x y]]
  (map vector [x (dec x) (inc x)] (repeat (inc y))))

(defn next-sand [floor points]
  (when-not (points cave-entrance)
    (loop [p cave-entrance]
      (if (= (second p) floor)
        p
        (if-let [p' (->> p possible-next-spaces (remove points) first)]
          (recur p')
          p)))))

(defn sand-seq [cave]
  (letfn [(find-next [floor points] (when-some [sand (next-sand floor points)]
                                      (lazy-seq (cons sand (find-next floor (conj points sand))))))]
    (find-next (cave-floor cave) cave)))

(defn part1 [input]
  (let [cave (parse-cave input)
        floor (cave-floor cave)]
    (->> (sand-seq cave)
         (take-while (fn [[_ y]] (< y floor)))
         count)))

(defn part2 [input]
  (->> (parse-cave input)
       sand-seq
       (take-while some?)
       count))
