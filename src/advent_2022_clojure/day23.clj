(ns advent-2022-clojure.day23
  (:require [advent-2022-clojure.point :as p]
            [advent-2022-clojure.utils :refer [first-when]]))

(defn parse-elves [input]
  (->> (p/parse-to-char-coords input)
       (keep (fn [[coords v]] (when (= v \#) coords)))
       set))

(def northward [[0 -1] [-1 -1] [1 -1]])
(def southward [[0 1] [-1 1] [1 1]])
(def westward [[-1 0] [-1 -1] [-1 1]])
(def eastward [[1 0] [1 -1] [1 1]])
(def direction-partitions (partition 4 1 (cycle [northward southward westward eastward])))

(defn propose-next-step [elves directions elf]
  (when-some [[dir] (and (some elves (p/surrounding elf))
                         (first-when (fn [dirs] (not-any? #(elves (mapv + elf %)) dirs)) directions))]
    (mapv + elf dir)))

(defn move-elves [elves directions]
  (let [state' (reduce (fn [{:keys [targeted blocked] :as acc} elf]
                         (if-let [elf' (propose-next-step elves directions elf)]
                           (cond
                             (blocked elf') (update acc :staying conj elf)
                             (targeted elf') (-> acc
                                                 (update :blocked conj elf')
                                                 (update :staying conj (targeted elf') elf)
                                                 (update :targeted dissoc elf'))
                             :else (update acc :targeted assoc elf' elf))
                           (update acc :staying conj elf)))
                       {:staying #{}, :targeted {}, :blocked #{}}
                       elves)]
    (into (:staying state') (keys (:targeted state')))))

(defn elves-seq
  ([starting-elves] (elves-seq starting-elves direction-partitions))
  ([elves direction-seq]
   (lazy-seq (cons elves (elves-seq (move-elves elves (first direction-seq))
                                    (rest direction-seq))))))

(defn part1 [input]
  (let [elves' (-> (parse-elves input)
                   (elves-seq)
                   (nth 10))
        [[x-min y-min] [x-max y-max]] (p/bounding-box elves')]
    (- (* (- (inc x-max) x-min) (- (inc y-max) y-min))
       (count elves'))))

(defn part2 [input]
  (->> (parse-elves input)
       (elves-seq)
       (partition 2 1)
       (keep-indexed (fn [idx [e1 e2]] (when (= e1 e2) idx)))
       first
       inc))