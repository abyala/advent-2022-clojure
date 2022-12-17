(ns advent-2022-clojure.day16
  (:require [clojure.string :as str]
            [clojure.set :as set]
            [advent-2022-clojure.utils :as u]
            [advent-2022-clojure.point :as p]))


(defn parse-tunnel [s]
  (let [[_ name rate tunnels] (re-matches #"Valve (\w+) has flow rate=(\d+); .* valves? (.*)" s)]
    [name {:rate (parse-long rate) :tunnels (str/split tunnels #", ")}]))
(defn parse-input [input]
  (into {} (map parse-tunnel (str/split-lines input))))

(def initial-state {:room "AA" :open #{} :rate 0 :released 0 :time-spent 0 :since-last-opened #{}})

(defn- open-valve-move [tunnel-map state]
  (let [{:keys [room open]} state
        room-rate (get-in tunnel-map [room :rate])]
    (when (and (not (open room))
               (pos? room-rate))
      (-> state
          (assoc :since-last-opened #{})
          (update :time-spent inc)
          (update :open conj room)
          (update :rate + room-rate)))))

(defn- follow-tunnels-move [tunnel-map state]
  (let [{:keys [room since-last-opened]} state]
    (->> (get-in tunnel-map [room :tunnels])
         (remove since-last-opened)
         (map #(-> state
                   (assoc :room %)
                   (update :since-last-opened conj %)
                   (update :time-spent inc))))))

(defn next-steps [tunnel-map state]
  (let [state' (update state :released + (:rate state))]
    (concat (follow-tunnels-move tunnel-map state')
            (keep identity [(open-valve-move tunnel-map state')]))))

(defn part1 [input]
  ; This is insanely slow (3.5 hours for the puzzle data), but it does work.
  (let [tunnel-map (parse-input input)]
    (loop [options [initial-state] most-pressure 0]
      (if-let [option (first options)]
        (if (= (:time-spent option) 30)
          (recur (rest options) (max most-pressure (:released option)))
          (recur (concat (next-steps tunnel-map option) (rest options)) most-pressure))
        most-pressure))))

;;(defn parse-tunnel [s]
;;  (let [[_ name rate tunnels] (re-matches #"Valve (\w+) has flow rate=(\d+); .* valves? (.*)" s)]
;;    [name {:rate (parse-long rate) :tunnels (str/split tunnels #", ")}]))
;;(defn parse-input [input]
;;  (into {} (map parse-tunnel (str/split-lines input))))
;;
;;(def initial-state {:room "AA" :open #{} :rate 0 :released 0 :time-spent 0})
;;
;;(defn next-step [tunnel-map room time-remaining opened]
;;  (if (zero? time-remaining)
;;    0
;;    (let [max-move-score (reduce max (map #(next-step tunnel-map % (dec time-remaining) opened)
;;                                          (get-in tunnel-map [room :tunnels])))
;;          open-option (if (opened room) 0
;;                                        (+ (* (dec time-remaining) (get-in tunnel-map [room :rate]))
;;                                           (next-step tunnel-map room (dec time-remaining) (conj opened room))))]
;;      (max max-move-score open-option))))
;;
;;(def next-step
;;  (memoize (fn [tunnel-map room time-remaining opened]
;;             (if (zero? time-remaining)
;;               0
;;               (let [max-move-score (reduce max (map #(next-step tunnel-map % (dec time-remaining) opened)
;;                                                     (get-in tunnel-map [room :tunnels])))
;;                     open-option (if (opened room) 0
;;                                                   (+ (* (dec time-remaining) (get-in tunnel-map [room :rate]))
;;                                                      (next-step tunnel-map room (dec time-remaining) (conj opened room))))]
;;                 (max max-move-score open-option)))))
;;  )
;;
;;(defn- open-valve-move [tunnel-map state]
;;  (let [{:keys [room open]} state
;;        room-rate (get-in tunnel-map [room :rate])]
;;    (when (and (not (open room))
;;               (pos? room-rate))
;;      (-> state
;;          (update :time-spent inc)
;;          (update :open conj room)
;;          (update :rate + room-rate)))))
;;
;;(defn- follow-tunnels-move [tunnel-map state]
;;  (let [{:keys [room since-last-opened]} state]
;;    (->> (get-in tunnel-map [room :tunnels])
;;         (remove since-last-opened)
;;         (map #(-> state
;;                   (assoc :room %)
;;                   (update :time-spent inc))))))
;;
;;(defn next-steps [tunnel-map state]
;;  (let [state' (update state :released + (:rate state))]
;;    (concat (follow-tunnels-move tunnel-map state')
;;            (keep identity [(open-valve-move tunnel-map state')]))))

#_(comment
  ; Slow implementation
  (time (let [tunnel-map (parse-input input)]
          (loop [options [initial-state] most-pressure 0]
            (if-let [option (first options)]
              (if (= (:time-spent option) 30)
                (recur (rest options) (max most-pressure (:released option)))
                (recur (concat (next-steps tunnel-map option) (rest options)) most-pressure))
              most-pressure))
          ))


  ; Seems to be going nowhere
  (nth (reductions (fn [acc [room-name {:keys [rate tunnels] :as room}]]
                     (println "Looking at" room-name "as" room)
                     (reduce #(add-route %1 %2 room-name)
                             (update-in acc [room-name :outgoing] into (map #(vector % 1) tunnels))
                             tunnels)

                     #_(reduce (fn [acc' tunnel] (if-let (acc' tunnel)
                                                   (update-in acc' [tunnel :outgoing ])
                                                   ))
                               (conj acc room)
                               tunnels))
                   (parse-input input)
                   ;;(map hash-map (keys (parse-input input)) (repeat {:incoming #{} :outgoing {}}))
                   (parse-input input))
       4)
  )
