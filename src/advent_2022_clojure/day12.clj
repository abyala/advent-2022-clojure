(ns advent-2022-clojure.day12
  (:require [advent-2022-clojure.point :as p]))

(def lowest-char \a)
(def start-char \S)
(def end-char \E)
(defn elevation [c] (or ({start-char 1, end-char 26} c)
                        (- (int c) 96)))

(defn filter-for-keys [value-pred coll]
  (keep (fn [[k v]] (when (value-pred v) k)) coll))

(defn parse-input [starting-characters input]
  (let [grid (p/parse-to-char-coords-map input)]
    {:grid   (reduce-kv #(assoc %1 %2 (elevation %3)) {} grid),
     :starts (filter-for-keys starting-characters grid)
     :target (first (filter-for-keys #{end-char} grid))}))

(defn climbable? [grid from to]
  (<= (grid to) (inc (grid from))))

(defn possible-steps [grid current]
  (filter (every-pred grid (partial climbable? grid current))
          (p/neighbors current)))

(defn shortest-path [grid target start]
  (loop [options [{:current start, :moves 0}], seen #{}]
    (let [{:keys [current moves]} (first options)]
      (cond
        (= current target) moves
        (seen current) (recur (subvec options 1) seen)
        :else (let [neighbors (possible-steps grid current)
                    neighbor-options (map #(hash-map :current %, :moves (inc moves)) neighbors)
                    combined-options (apply conj options neighbor-options)]
                (when (seq combined-options)
                  (recur (subvec (apply conj options neighbor-options) 1) (conj seen current))))))))

(defn solve [starting-chars input]
  (let [{:keys [grid starts target]} (parse-input starting-chars input)]
    (->> starts
         (keep (partial shortest-path grid target))
         (reduce min))))

(defn part1 [input] (solve #{start-char} input))
(defn part2 [input] (solve #{start-char lowest-char} input))
