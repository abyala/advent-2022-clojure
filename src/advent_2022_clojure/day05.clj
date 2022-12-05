(ns advent-2022-clojure.day05
  (:require
    [advent-2022-clojure.utils :as utils]))

(defn parse-crane-line [line]
  (mapv (comp #(when (not= % \space) %) second)
        (partition-all 4 line)))

(defn parse-crane [s]
  (let [parsed (map parse-crane-line s)
        rows (butlast parsed)
        names (last parsed)]
    (reduce (fn [acc idx]
              (assoc acc (get names idx)
                         (keep identity (map #(get % idx) rows))))
            {}
            (range (count names)))))

(defn parse-instruction [s]
  (let [[quantity from to] (re-seq #"\d+" s)]
    {:quantity (parse-long quantity) :from (first from) :to (first to)}))

(defn parse [input]
  (let [[crane-str instruction-str] (utils/split-blank-line-groups input)]
    {:crane        (parse-crane crane-str)
     :instructions (map parse-instruction instruction-str)}))

(defn apply-instruction [one-at-a-time? crane {:keys [from to quantity]}]
  (let [crane-fn (if one-at-a-time? identity reverse)
        moving (crane-fn (take quantity (get crane from)))]
    (-> crane
        (update to #(apply conj % moving))
        (update from #(drop quantity %)))))

(defn apply-all-instructions [one-at-a-time? crane instructions]
  (reduce (partial apply-instruction one-at-a-time?) crane instructions))

(defn top-of-crane [crane]
  (->> crane vals (map first) (apply str)))

(defn solve [one-at-a-time? input]
  (let [{:keys [crane instructions]} (parse input)]
    (top-of-crane (apply-all-instructions one-at-a-time? crane instructions))))

(defn part1 [input] (solve true input))
(defn part2 [input] (solve false input))