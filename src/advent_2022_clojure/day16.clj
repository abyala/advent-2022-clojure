(ns advent-2022-clojure.day16
  (:require [clojure.string :as str]))

(defn parse-tunnel [s]
  (let [[_ name rate tunnels] (re-matches #"Valve (\w+) has flow rate=(\d+); .* valves? (.*)" s)
        neighbors (reduce #(assoc %1 %2 1) {} (str/split tunnels #", "))]
    [name {:rate (parse-long rate) :tunnels neighbors}]))

(defn parse-input [input]
  (into {} (map parse-tunnel (str/split-lines input))))

(defn all-connections [parsed-connections]
  (let [initial-connections (reduce (fn [acc k] (assoc-in acc [k :tunnels] {}))
                                    parsed-connections
                                    (keys parsed-connections))
        initial-lessons (into {} (mapcat (fn [[from {:keys [tunnels]}]]
                                           (map #(vector [from %] 1) (keys tunnels)))
                                         parsed-connections))]
    (loop [connections initial-connections, lessons initial-lessons]
      (if (seq lessons)
        (let [[[from to :as journey] cost] (first lessons)]
          (if (> (get-in connections [from :tunnels to] Integer/MAX_VALUE) cost)
            (let [new-lessons (reduce (fn [acc [nested-dest nested-cost]]
                                        (assoc acc [nested-dest to] (+ nested-cost cost)
                                                   [to nested-dest] (+ nested-cost cost)))
                                      {}
                                      (dissoc (get-in connections [from :tunnels]) to))]
              (recur (update-in connections [from :tunnels] assoc to cost)
                     (merge-with min (dissoc lessons journey) new-lessons)))
            (recur connections (dissoc lessons journey))))
        connections))))

(defn restrict-paths-to-rooms [tunnel-map allowed-rooms]
  (reduce (fn [m k] (if (or (allowed-rooms k) (= "AA" k))
                      (update m k update :tunnels select-keys allowed-rooms)
                      (dissoc m k)))
          tunnel-map
          (keys tunnel-map)))

(defn remove-broken-valve-rooms [tunnel-map]
  (->> tunnel-map
       (keep (fn [[name {:keys [rate]}]] (when (pos-int? rate) name)))
       set
       (restrict-paths-to-rooms tunnel-map)))

(defn prepare-tunnel-map [input]
  (-> (parse-input input) all-connections remove-broken-valve-rooms))

(defn initial-state [total-time]
  {:room "AA" :open #{} :rate 0 :released 0 :time-remaining total-time})

(defn follow-tunnels-move [tunnel-map state]
  (let [{:keys [room open time-remaining]} state]
    (keep (fn [[room' dist]] (let [time-after-move-and-open (- time-remaining dist 1)]
                               (when (and (not (open room'))
                                          (>= time-after-move-and-open 0))
                                 (let [released-in-transit (* (inc dist) (:rate state))]
                                   (-> (assoc state :room room' :time-remaining time-after-move-and-open)
                                       (update :open conj room')
                                       (update :released + released-in-transit)
                                       (update :rate + (get-in tunnel-map [room' :rate])))))))
          (get-in tunnel-map [room :tunnels]))))

(defn give-up-move [state]
  (let [{:keys [rate time-remaining]} state]
    (-> (assoc state :time-remaining 0)
        (update :released + (* time-remaining rate)))))

(defn next-steps [tunnel-map state]
  (let [options (follow-tunnels-move tunnel-map state)]
    (if (seq options)
      options
      [(give-up-move state)])))

(defn final-states [options tunnel-map]
   (when (seq options)
    (let [[option & others] options]
      (if (zero? (:time-remaining option))
        (lazy-seq (cons option (final-states others tunnel-map)))
        (recur (into others (next-steps tunnel-map option)) tunnel-map)))))

(defn max-pressure-released [max-steps tunnel-map]
  (transduce (map :released) max 0 (final-states (list (initial-state max-steps)) tunnel-map)))

(defn part1 [input]
  (max-pressure-released 30 (prepare-tunnel-map input)))

(defn split-across-pairs [values]
  (reduce (fn [acc room] (concat (map #(update % 0 conj room) acc)
                                 (map #(update % 1 conj room) acc)))
          [[[(first values) "AA"] ["AA"]]]
          (rest values)))

(defn tunnel-pairs [tunnel-map]
  (map (fn [pair] (map #(restrict-paths-to-rooms tunnel-map (set %)) pair))
       (split-across-pairs (keys tunnel-map))))

(defn max-pressure-for-pair [pair]
  (apply + (map (partial max-pressure-released 26) pair)))

(defn part2 [input]
  (let [tunnel-map (prepare-tunnel-map input)]
    (->> (tunnel-pairs tunnel-map)
         (map max-pressure-for-pair)
         (reduce max))))