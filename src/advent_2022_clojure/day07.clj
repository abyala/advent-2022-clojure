(ns advent-2022-clojure.day07
  (:require [clojure.string :as str]))

(def root-path [])
(def empty-directory {:size 0, :dirs {}})

(defn move-up [current-dir]
  (subvec current-dir 0 (max 0 (- (count current-dir) 2))))

(defn move-down [current-dir sub-dir]
  (apply conj current-dir [:dirs sub-dir]))

(defn create-directory [state current-dir dir-name]
  (assoc-in state (apply conj current-dir [:dirs dir-name]) empty-directory))

(defn create-file [state current-dir size]
  (update-in state (conj current-dir :size) + size))

(defn parse-input [input]
  (loop [lines (str/split-lines input), state empty-directory, current-dir root-path]
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

(defn walk-dir-sizes
  ([fs] (walk-dir-sizes fs root-path))
  ([fs loc]
   (let [child-dir-names (keys (get-in fs (conj loc :dirs)))
         local-file-size (get-in fs (conj loc :size))
         sub-dirs (reduce (fn [acc child] (->> (move-down loc child) (walk-dir-sizes fs) (merge acc)))
                          {}
                          child-dir-names)
         sub-dir-sizes (transduce (map (comp sub-dirs (partial move-down loc))) + child-dir-names)]
     (assoc sub-dirs loc (+ sub-dir-sizes local-file-size)))))

(defn input->sizes [input]
  (-> input parse-input walk-dir-sizes vals))

(defn part1 [input]
  (transduce (filter (partial >= 100000)) + (input->sizes input)))

(defn part2 [input]
  (let [sizes (input->sizes input)
        target (- (reduce max sizes) 40000000)]
    (reduce min (filter (partial < target) sizes))))
