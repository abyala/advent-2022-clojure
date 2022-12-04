# Day 4: Camp Cleanup

* [Problem statement](https://adventofcode.com/2022/day/4)
* [Solution code](https://github.com/abyala/advent-2022-clojure/blob/master/src/advent_2022_clojure/day04.clj)

---

## Intro

Today's puzzle reminded me of an old interview question my company used to give.  Shhhh! Don't tell anyone!

---

## Part One

In the first part of this puzzle, we need look at lines that contain two integer ranges, and return whether either of
those ranges fully contains the other. First, let's parse the input.

```clojure
(defn parse-line [line]
  (map parse-long (re-seq #"\d+" line)))
```

The `parse-line` function does two things to the input string. First, we use `re-seq` to apply a regular expression to
the input and return a sequence of all matches. Since each line has a format like `2-4,6-8` of non-negative numbers,
the regular expression is simply `#"\d+"` where `#"` starts a regex. Then for each numeric string returned, we `map`
it using `parse-long`. I forgot to mention in puzzle days how much I appreciate that Clojure 1.11 finally has
`parse-long` in the core namespace!

Now that we have a sequence of numbers for each line, we need to determine whether either of the ranges entirely
contains the other, and for that we'll create `fully-contains?`.

```clojure
; My preferred solution
(defn fully-contains? [[a b c d]]
  (or (<= a c d b) (<= c a b d)))

; Equivalent, slightly faster, but slightly uglier solution
(defn fully-contains? [[a b c d]]
  (or (and (<= a c) (<= d b))
      (and (<= c a) (<= b d))))
```

To start off, we'll destructure the incoming sequence into its four numeric components by wrapping the first and only
function argument with an extra set of square brackets. In this case, `a` and `b` are the low and high values for the
first range, and `c` and `d` are the low and high values for the second. If one range, let's say `a-b` contains all of
range `c-d`, then there's a specific order of values that must appear. We need `a<=c`, `c<=d` (which we already know 
because `c` and `d` make a range), and `d<= b`. While we could write combine a few `and` and `or` expressions for the
first and third comparisons for both range orders, the `<=` function supports variable arities, so `(<= a c d b)` gets
the job done cleanly at the cost of one unnecessary comparison.

Before solving this, I recognize that I'm starting to get Kotlin Stdlib envy from my coworkers, so I decided it was
time to implement my own `count-if` function. It's just a wrapper of `count` around a filter, but it's pretty.

```clojure
; advent-2022-clojure.utils namespace
(defn count-if 
  "Returns the number of items in a collection that return a truthy response to a predicate filter."
  [pred coll]
  (count (filter pred coll)))
```

We're ready to solve this with the `part1` function by splitting and parsing each line, and calling the new
`count-if` function with the `fully-contains?` predicate.

```clojure
(defn part1 [input]
  (->> (str/split-lines input)
       (map parse-line)
       (u/count-if fully-contains?)))
```

---

## Part Two

Oh, well this is easy. Instead of checking if either range fully contains the other, we just need to see if the two
ranges overlap at all. So let's create an `overlaps?` function, and then we'll be almost done. Let's start with the
naive implementation first.

```clojure
(defn overlaps? [[a b c d]]
  (or (<= a c b)
      (<= a d b)
      (<= c a d)
      (<= c b d)))
```

To see if there's any overlap between two segments, we just need to see whether one of the low or high points of one
range fall between the low and high points of the other range. So for example, in the case of `2-6,4-8`, it's true that
both `6` is within `4-8`, and `4` is within `2-6`.

If we look at all of the possible relationships where the two ranges overlap, we start to see a few patterns; note
that I will continue to use the `a-d` bindings for simplicity.

* Fully contained: For `2-8,3-7`, we see that both `c` and `d` are within `a-b`.
* Overlap on the edge: For `5-7,7-9`, we see that `b` is within `c-d` and `c` is within `a-b`.
* Partial overlap: For `2-6,4-8`, we again see that `b` is within `c-d` and `c` is within `a-b`.

So what can we learn from this? We actually don't have to do four comparisons at all! If we do the same check from
range 1 to range 2, and then again from range 2 to range 1, we only need to do two comparisons in total. Either of
the below solutions are equivalent to the one above:

```clojure
; Check against the other range's low value (a and c)
(defn overlaps? [[a b c d]]
  (or (<= a c b) (<= c a d)))

; Check against the other range's high value (b and d)
(defn overlaps? [[a b c d]]
  (or (<= a d b) (<= c b d)))
```

Any of the above functions should work. But now we can implement the `part2` function. It's identical to the `part1`
function, except that it uses `overlaps?` instead of `fully-contains?`.

```clojure
(defn part2 [input]
  (->> (str/split-lines input)
       (map parse-line)
       (u/count-if overlaps?)))
```

You know this is coming - let's refactor the two functions to use a common `solve` function.

```clojure
(defn solve [pred input]
  (->> (str/split-lines input)
       (map parse-line)
       (u/count-if pred)))

(defn part1 [input] (solve fully-contains? input))
(defn part2 [input] (solve overlaps? input))
```

