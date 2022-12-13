(ns advent-2022-clojure.utils
  (:require [clojure.string :as str]))

(def block-char \█)

(defn split-by-blank-lines
  "Given an input string, returns a sequence of sub-strings, separated by a completely
  blank string. This function preserves any newlines between blank lines, and it filters
  out Windows' \"\r\" characters."
  [input]
  (-> (str/replace input "\r" "")
      (str/split #"\n\n")))

(defn split-blank-line-groups
  "Given an input string that represents multiple lines that get grouped together with blank line separators,
  returns a sequence of the line groups. Each line within a line group can optionally have a transformation
  function applied to it before being returned."
  ([input] (split-blank-line-groups identity input))
  ([xf input] (->> (str/split-lines input)
                   (partition-by str/blank?)
                   (take-nth 2)
                   (map (partial map xf)))))

(defn count-if
  "Returns the number of items in a collection that return a truthy response to a predicate filter."
  [pred coll]
  (count (filter pred coll)))

(defn index-of-first
  "Returns the index of the first value in a collection that returns a truthy response to a predicate filter."
  [pred coll]
  (first (keep-indexed #(when (pred %2) %1) coll)))

(defn char->int
  "Reads a numeric character and returns its integer value, assuming base 10."
  [^Character c]
  (Character/digit c 10))

(defn take-until
  "Returns all values in the input collection for which the predicate is falsey, plus the first one that is truey (if
  any). Returns nil for a nil or empty input collection."
  [pred coll]
  (when (seq coll)
    (let [[x & xs] coll]
      (if (pred x) (list x) (lazy-seq (cons x (take-until pred xs)))))))

(defn divisible? [num denom]
  (zero? (rem num denom)))

(defn signum
  "Given a number `n`, returns -1, 0, or 1 based on if the number is negative, zero, or positive."
  [n]
  (cond (zero? n) 0
        (neg? n) -1
        :else 1))