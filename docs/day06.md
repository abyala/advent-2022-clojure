# Day 6: Tuning Trouble

* [Problem statement](https://adventofcode.com/2022/day/6)
* [Solution code](https://github.com/abyala/advent-2022-clojure/blob/master/src/advent_2022_clojure/day06.clj)

---

## Intro

This might just be the simplest advent puzzle I've seen in a very long time, even compared with the day 1 puzzles. I
choose to be grateful that I'll get more sleep tonight that most other days during this event!

---

## Part One

The puzzle is asking us to look for the first four characters in an input string where all values are different, and
return the index of the last character in the string (1-indexed). So the smallest possible value returned would be a 
4 if the first four characters were unique.

We don't need any parsing logic, or even any helper functions. This all fits nicely into a single, small function.

```clojure
(defn part1 [input]
  (->> (partition-all 4 1 input)
       (keep-indexed (fn [idx letters] (when (= 4 (count (set letters)))
                                         (+ 4 idx))))
       first))
```

Let's look at this line by line. First, we call `(partition 4 1 input)`, which breaks the string into 4-element
partitions, which in this case means sequences of four characters. Since we set the step value to `1`, that means that
we want every overlapping 4-character sequence; if we left that parameter out, then the first sequence of the alphabet
would be `(\a \b \c \d)` but the second would be `(\e \f \g \h)`, where we'd want to see `(\b \c \d \e)`.

Then we use the `keep-indexed` function, which applies a mapping function to a collection and throws away any `nil`
results. The mapping function takes in the index of the collection (0-indexed) and the value, which in this case is the
sequence of letters. The easiest way to know if all four characters are unique is to convert the sequence into a set
and check if it still has four values when we're done, since a set cannot have duplicates. If it's a match, we take the
index (the starting index), add 4, and return the first value.

---

## Part Two

Well this is silly. We need to do the same thing, but look for the index ending the first 14 non-overlapping values.
So the first thing to do is simply do a copy-paste job and change the `4`s to `14`s.

```clojure
(defn part2 [input]
  (->> (partition-all 14 1 input)
       (keep-indexed (fn [idx letters] (when (= 14 (count (set letters)))
                                         (+ 14 idx))))
       first))
```

With a tiny bit of effort, we then see we can parameterize the sequence length and refactor the problem into a very
simple solution.

```clojure
(defn solve [n input]
  (->> (partition-all n 1 input)
       (keep-indexed (fn [idx letters] (when (= n (count (set letters)))
                                         (+ n idx))))
       first))

(defn part1 [input] (solve 4 input))
(defn part2 [input] (solve 14 input))
```

---

## Refactoring

Discussing solutions with my coworkers, I was impressed by
(Matt Kuhn's solution)[https://github.com/mtkuhn/advent-of-code-2022/blob/main/src/main/kotlin/mkuhn/aoc/Day06.kt], and
his use of Kotlin's (indexOfFirst)[https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/index-of-first.html]
standard library function. Clojurists tend to use lots of little functions instead of necessarily building convenience
functions like this, but who says I need to live that way?

Let's build a simple `index-of-first` function for generic use.

```clojure
(defn index-of-first
  "Returns the index of the first value in a collection that returns a truthy response to a predicate filter."
  [pred coll]
  (first (keep-indexed #(when (pred %2) %1) coll)))
```

Unlike how Java and Kotlin tend to return `-1` when an indexed value isn't available, due to the use of primitives,
but in Clojure it's much more common to use `nil`. It's reasonable to say that `(solve pred [])` should return `nil`
instead of `-1`.

With that done, we can make the `solve` function even easier to look at.

```clojure
(defn solve [n input]
  (->> (partition-all n 1 input)
       (index-of-first #(= n (count (set %))))
       (+ n)))
```

One quick note - if we were to get invalid data, such as `aaaaaaaaa` for part 1, the code will throw a
`NullPointerException` because `(+ nil 4)` and `(+ 4 nil)` fails. My original solution would have returned `nil`,
which is still unexpected, but is a nicer value. Note that in the Java/Kotlin world, if `indexOfFirst` returned -1,
then adding 4 to it would have returned an answer of 3, which is misleadingly false.

If we want to handle this scenario with `index-of-first`, then the code would still be straightforward with the use of
`when-let`. I don't program Advent problems overly defensively, so I won't do this in the solution I keep, but it's
here for completeness.

```clojure
(defn solve [n input]
  (when-let [index (index-of-first #(= n (count (set %)))
                                   (partition-all n 1 input))]
    (+ index n)))
```