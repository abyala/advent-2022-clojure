(ns advent-2022-clojure.day02
  (:require [clojure.string :as str]))

(def shape-map {\A :rock, \B :paper, \C :scissors
                \X :rock, \Y :paper, \Z :scissors})
(def outcome-map {\X :loss \Y :draw \Z :win})
(def shape-rules {:rock     {:points  1
                             :against {:rock :draw, :paper :loss, :scissors :win}}
                  :paper    {:points  2
                             :against {:rock :win, :paper :draw, :scissors :loss}}
                  :scissors {:points  3
                             :against {:rock :loss, :paper :win, :scissors :draw}}})
(def outcome-rules {:loss {:points  0
                           :against {:rock :scissors, :paper :rock, :scissors :paper}}
                    :draw {:points  3
                           :against {:rock :rock, :paper :paper, :scissors :scissors}}
                    :win  {:points  6
                           :against {:rock :paper, :paper :scissors, :scissors :rock}}})

(defn parse-line [line]
  (let [[c1 _ c2] line]
    {:shape1   (shape-map c1)
     :shape2   (shape-map c2)
     :outcome2 (outcome-map c2)}))

(defn score-round [shape outcome]
  (+ (get-in shape-rules [shape :points])
     (get-in outcome-rules [outcome :points])))

(defn run-round [shape-fn outcome-fn line]
  (let [round (parse-line line)]
    (score-round (shape-fn round) (outcome-fn round))))

(defn solve [shape-fn outcome-fn input]
  (transduce (map (partial run-round shape-fn outcome-fn)) + (str/split-lines input)))

(defn parsed-shape1 [round] (:shape1 round))
(defn parsed-shape2 [round] (:shape2 round))
(defn parsed-outcome2 [round] (:outcome2 round))
(defn derived-shape2 [round]
  (get-in outcome-rules [(parsed-outcome2 round) :against (parsed-shape1 round)]))
(defn derived-outcome2 [round]
  (get-in shape-rules [(parsed-shape2 round) :against (parsed-shape1 round)]))

(defn part1 [input] (solve parsed-shape2 derived-outcome2 input))
(defn part2 [input] (solve derived-shape2 parsed-outcome2 input))
