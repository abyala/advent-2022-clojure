(ns advent-2022-clojure.day10
  (:require [clojure.string :as str]
            [advent-2022-clojure.utils :refer [block-char]]))

(def crt-width 40)

(defn to-instructions [s]
  (if (= s "noop")
    [identity]
    [identity (partial + (-> s (str/split #" ") second parse-long))]))

(defn signal-strengths [input]
  (->> (str/split-lines input)
       (mapcat to-instructions)
       (reductions #(%2 %1) 1)
       vec))

(defn part1 [input]
  (let [signals (signal-strengths input)]
    (transduce (map (fn [offset] (* offset (signals (dec offset))))) + [20 60 100 140 180 220])))

(defn print-character [crt-cycle signal]
  (if (<= (abs (- crt-cycle signal)) 1) block-char \space))

(defn part2 [input]
  (->> (map print-character (signal-strengths input) (cycle (range crt-width)))
       (partition crt-width)
       (map (partial apply str))
       (run! println)))
