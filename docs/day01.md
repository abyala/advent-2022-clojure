# Day 1: Calorie Counting

* [Problem statement](https://adventofcode.com/2022/day/1)
* [Solution code](https://github.com/abyala/advent-2022-clojure/blob/master/src/advent_2022_clojure/day01.clj)

---

## Part One

Our first puzzle is a fairly simple one, which involves summing together groups of numbers that represent calorie
counts, and finding the largest sum.  Not too bad for starters.

The first step, as always, is parsing. For this, I'm leveraging a function I wrote for a previous Advent Of Code year
called `split-blank-line`, which returns a sequence of strings, one for each entire groupings of lines of text that are
split by a blank line. For the test data, it will return a sequence starting with
`("1000\n2000\3000" "4000" "5000\n6000")`.

```clojure
(defn split-blank-line
  "Given an input string, returns a sequence of sub-strings, separated by a completely
  blank string. This function preserves any newlines between blank lines, and it filters
  out Windows' \"\r\" characters."
  [input]
  (-> (str/replace input "\r" "")
      (str/split #"\n\n")))
```

For each new string that represents each of the items' calories the elf is bringing, we need to add parse the string
and add the results together. This is a perfect case for the `transduce` function, which combines a transformation
function and a reducing function together. Here, we feed `(str/split-lines s)` in, so we get a sequence of numeric
strings. The transformation function is `(map parse-long)` to convert each substring into a number, and the reducing
function is simply the `+` function.

```clojure
(defn parse-calories [s]
  (transduce (map parse-long) + (str/split-lines s)))
```

Now to solve this puzzle, we split the input, map each elf's data using `parse-calories`, and find the max value
using `reduce max`. Easy!

```clojure
(defn part1 [input]
  (->> (utils/split-blank-line input)
       (map parse-calories)
       (reduce max)))
```

Note that if we wanted to, we could use yet another `transduce` function! Given the input data of
`(utils/split-blank-line input)`, a transformation function of `(map parse-calories)` and a reducing function
of `max`, with an initial value of `0`, we can represent the `part1` function in a single, succinct line!

```clojure
(defn part1 [input]
  (transduce (map parse-calories) max 0 (utils/split-blank-line input)))
```

---

## Part Two

In part 2, instead of finding the single largest value, we need the top 3 largest values.  We _could_ still use a
`transduce` function if we really wanted it, but it would be gross so let's not and instead start with the original
`part1` function as our working model.

And the solution here is pretty simple. Starting with our mapped sequence of parsed calorie counts, we just have 
to sort the results in decreasing order using `(sort >)`, grab the first three values with `(take 3)`, and then 
sum them together using `(take 3)` and `(reduce +)`.

```clojure
; Preferred solution
(defn part2 [input]
  (->> (utils/split-blank-line input)
       (map parse-calories)
       (sort >)
       (take 3)
       (reduce +)))
```

---

## Refactoring

It shouldn't be too hard to see how to combine these two functions into a single `solve` function - if we make our
sequence of sorted, summed calories, we just take the first `n` values (1 for part one, 3 for part two) and add them
together. So `part1` is simply `(solve 1 input)` and `part2` is `(solve 3 input)`.

```clojure
(defn solve [n input]
  (->> (utils/split-blank-line input)
       (map parse-calories)
       (sort >)
       (take n)
       (reduce +)))

(defn part1 [input] (solve 1 input))
(defn part2 [input] (solve 3 input))
```