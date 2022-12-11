(ns advent-2022-clojure.day07-eager
  (:require [clojure.string :as str]))

(def root-path (list "/"))
(def empty-directory {:size 0, :dirs ()})

(defn move-up [current-dir]
  (rest current-dir))

(defn move-down [current-dir sub-dir]
  (conj current-dir sub-dir))

(defn create-directory [state current-dir dir-name]
  (-> state
      (update-in [current-dir :dirs] conj (conj current-dir dir-name))
      (assoc (move-down current-dir dir-name) empty-directory)))

(defn create-file [state current-dir size]
  (if (seq current-dir)
    (recur (update-in state [current-dir :size] + size) (rest current-dir) size)
    state))

(defn parse-input [input]
  (loop [lines (str/split-lines input), state {root-path empty-directory}, current-dir root-path]
    (if-some [line (first lines)]
      (let [[arg0 arg1 arg2] (str/split line #" ")]
        (if (= arg0 "$")
          (recur (rest lines) state (cond (= arg1 "ls") current-dir
                                          (= arg2 "/") root-path
                                          (= arg2 "..") (move-up current-dir)
                                          :else (move-down current-dir arg2)))
          (recur (rest lines)
                 (if (= arg0 "dir")
                   (create-directory state current-dir arg1)
                   (create-file state current-dir (parse-long arg0)))
                 current-dir)))
      state)))

(defn input->sizes [input]
  (->> input parse-input vals (map :size)))

(defn part1 [input]
  (transduce (filter (partial >= 100000)) + (input->sizes input)))

(defn part2 [input]
  (let [sizes (input->sizes input)
        target (- (reduce max sizes) 40000000)]
    (reduce min (filter (partial < target) sizes))))
