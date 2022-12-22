(ns advent-2022-clojure.day21
  (:require [clojure.string :as str]))

(def root "root")
(def human "humn")
(defn op->fn [s] ({"+" +, "-" -, "*" *, "/" quot, "=" =} s))

(defn dependencies-of [state name]
  (get-in state [:monkeys name :dependencies]))

(defn value-of [{:keys [values]} name]
  (or (values name) name))

(defn delete-monkey [state name]
  (update state :monkeys dissoc name))

(defn record-monkey-value [state name value]
  (-> state
      (delete-monkey name)
      (assoc-in [:values name] value)))

(defn record-monkey-deps [state name deps]
  (assoc-in state [:monkeys name :dependencies] deps))

(defn parse-input [input]
  (reduce (fn [acc line] (let [name (subs line 0 4)
                               instruction (subs line 6)]
                           (if-some [n (parse-long instruction)]
                             (record-monkey-value acc name n)
                             (let [[m1 op m2] (str/split instruction #" ")]
                               (assoc-in acc [:monkeys name] {:dependencies [m1 m2] :op op})))))
          {:monkeys {} :values {}}
          (str/split-lines input)))

(defn simplify
  ([state] (simplify state root))
  ([state name]
   (if (get-in state [:values name])
     state
     (if-some [deps (dependencies-of state name)]
       (let [state' (reduce simplify state deps)
             values' (map (partial value-of state') deps)
             op (get-in state' [:monkeys name :op])]
         (if (and (every? number? values') (not= op "="))
           (record-monkey-value state' name (apply (op->fn op) values'))
           (record-monkey-deps state' name values')))
       state))))

(defn merged-numeric-value [parent-number child-number child-number-first? child-op]
  (cond
    (= child-op "+")                           (- parent-number child-number)
    (= child-op "*")                           (quot parent-number child-number)
    (and (= child-op "-") child-number-first?) (- child-number parent-number)
    (= child-op "-")                           (+ parent-number child-number)
    child-number-first?                        (quot child-number parent-number)
    :else                                      (* parent-number child-number)))

(defn adjust-for-equality [state]
  (let [[d1 d2] (dependencies-of state root)
        left-numeric? (number? d1)
        child-node-name (if left-numeric? d2 d1)
        numeric-value (if left-numeric? d1 d2)]
    (if-some [{child-deps :dependencies, child-op :op} (get-in state [:monkeys child-node-name])]
      (let [[child-d1 child-d2] child-deps
            left-child-numeric? (number? child-d1)
            grandchild-node-name (if left-child-numeric? child-d2 child-d1)
            child-numeric-value (if left-child-numeric? child-d1 child-d2)
            merged-value (merged-numeric-value numeric-value child-numeric-value left-child-numeric? child-op)]
        (recur (-> state
                   (delete-monkey child-node-name)
                   (record-monkey-deps root [grandchild-node-name merged-value]))))

      ; If there is no child, then this is the terminal match on the root equality, so bind it.
      (record-monkey-value state child-node-name numeric-value))))

(defn part1 [input]
  (-> (parse-input input)
      simplify
      (get-in [:values root])))

(defn part2 [input]
  (-> (parse-input input)
      (assoc-in [:monkeys root :op] "=")
      (update :values dissoc human)
      simplify
      adjust-for-equality
      (get-in [:values human])))
