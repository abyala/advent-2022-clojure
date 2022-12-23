(ns advent-2022-clojure.day22
  (:require [advent-2022-clojure.point :as p]
            [advent-2022-clojure.utils :refer [index-of-first]]
            [advent-2022-clojure.utils :as u]))

(def top-row 1)
(defn walkable? [board coords] (= :space (board coords)))

(defn parse-board [s]
  (reduce (fn [acc [coords v]] (if-some [v' ({\# :wall \. :space} v)]
                                 (assoc acc (mapv inc coords) v')
                                 acc))
          {}
          (p/parse-to-char-coords s)))

(defn parse-instructions [s]
  (map (partial (some-fn parse-long {"L" :left "R" :right})) (re-seq #"\d+|\w" s)))

(defn parse-input [input]
  (let [[board-str instruction-str] (u/split-by-blank-lines input)]
    {:board (parse-board board-str) :instructions (parse-instructions instruction-str)}))

(defn starting-pos [board]
  [(->> (keys board)
        (keep (fn [[x y]] (when (= y top-row) x)))
        (reduce min)) top-row])

(defn starting-player [board]
  {:pos (starting-pos board) :dir p/right})

(def directions [[0 -1] [1 0] [0 1] [-1 0]])
(defn turn [player turn-dir]
  (let [current-idx (index-of-first (partial = (:dir player)) directions)]
    (assoc player :dir (-> current-idx (+ ({:right 1 :left -1} turn-dir)) (mod 4) directions))))

(defn next-step [p dir] (mapv + p dir))
(defn opposite-dir [dir] (mapv * [-1 -1] dir))
(defn wrapped-pos [board {:keys [dir pos]}]
  (let [dir' (opposite-dir dir)]
    (->> (iterate #(next-step % dir') pos)
         (take-while board)
         last)))

(defn move [board {:keys [pos dir] :as player} steps]
  (if (zero? steps)
    player
    (let [pos' (next-step pos dir)]
      (case (board pos')
        :space (recur board (assoc player :pos pos') (dec steps))
        :wall player
        (let [opposite-pos (wrapped-pos board player)]
          (if (walkable? board opposite-pos)
            (recur board (assoc player :pos opposite-pos) (dec steps))
            player))))))

(defn follow-instruction [board player instruction]
  (if (number? instruction)
    (move board player instruction)
    (turn player instruction)))

(defn password [{:keys [pos dir]}]
  (+ (* 1000 (second pos))
     (* 4 (first pos))
     ({[1 0] 0, [0 1] 1, [-1 0] 2, [0 -1] 3} dir)))

(defn part1 [input]
  (let [{:keys [board instructions]} (parse-input input)]
    (->> instructions
         (reduce (fn [[board player] instruction] [board (follow-instruction board player instruction)])
                 [board (starting-player board)])
         second
         password)))
