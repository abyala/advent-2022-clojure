(ns advent-2022-clojure.day24
  (:require [clojure.core.match :as match]
            [advent-2022-clojure.point :as p]
            [advent-2022-clojure.utils :refer [take-until]]))

(defn parse-valley [input]
  (let [points (p/parse-to-char-coords input)
        blizzards (vec (keep (fn [[coords v]] (when-some [dir ({\> [1 0] \< [-1 0] \^ [0 -1] \v [0 1]} v)]
                                                [coords dir])) points))
        spaces-by-row (group-by second (keep #(when (= \. (second %)) (first %)) points))
        max-row (apply max (keys spaces-by-row))
        exit (first (spaces-by-row max-row))]
    {:blizzards blizzards
     :entrance  [1 0]
     :exit      exit
     :max-x     (first exit)
     :max-y     (dec (second exit))}))

(defn move-blizzard [max-x max-y [p dir :as blizzard]]
  (let [wall-x (inc max-x)
        wall-y (inc max-y)]
    (assoc blizzard 0 (match/match (mapv + p dir)
                                   [0 y] [max-x y]
                                   [wall-x y] [1 y]
                                   [x 0] [x max-y]
                                   [x wall-y] [x 1]
                                   [x y] [x y]))))

(defn move-blizzards [{:keys [blizzards max-x max-y] :as valley}]
  (assoc valley :blizzards (map (partial move-blizzard max-x max-y) blizzards)))

(defn blizzard-set-seq [valley]
  (->> (iterate move-blizzards valley)
       (map (comp set (partial map first) :blizzards))))

(defn expedition-move-options [{:keys [blizzards entrance exit max-x max-y]} expedition]
  (filter (fn [[x y :as p]]
            (or (= p expedition entrance)
                (= p exit)
                (and (not (blizzards p))
                     (<= 1 x max-x)
                     (<= 1 y max-y))))
          (conj (p/neighbors expedition) expedition)))

(defn expedition-options-seq
  ([valley]
   (expedition-options-seq valley (rest (blizzard-set-seq valley))))

  ([valley [blizzards & next-blizzards]]
   (expedition-options-seq (assoc valley :blizzards blizzards) next-blizzards #{(:entrance valley)}))

  ([valley [blizzards & next-blizzards] expedition]
   (lazy-seq (cons expedition
                   (expedition-options-seq (assoc valley :blizzards blizzards)
                                           next-blizzards
                                           (set (mapcat (partial expedition-move-options valley) expedition)))))))

(defn steps-to-exit [valley blizzard-seq]
  (->> (expedition-options-seq valley blizzard-seq)
       (take-until #(% (:exit valley)))
       count
       dec))

(defn expedition-swap-seq
  ([valley] (expedition-swap-seq valley (rest (blizzard-set-seq valley))))
  ([valley blizzard-set]
   (let [num-steps (steps-to-exit valley blizzard-set)
         [entrance' exit'] ((juxt :exit :entrance) valley)]
     (lazy-seq (cons num-steps (expedition-swap-seq (assoc valley :entrance entrance' :exit exit')
                                                    (drop num-steps blizzard-set)))))))

(defn solve [num-journeys input]
  (->> (parse-valley input)
       (expedition-swap-seq)
       (take num-journeys)
       (reduce +)))

(defn part1 [input] (solve 1 input))
(defn part2 [input] (solve 3 input))