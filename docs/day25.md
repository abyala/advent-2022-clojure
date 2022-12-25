# Day 25: Full of Hot Air

* [Problem statement](https://adventofcode.com/2022/day/25)
* [Solution code](https://github.com/abyala/advent-2022-clojure/blob/master/src/advent_2022_clojure/day25.clj)

---

## Intro

The last puzzle! Well... the last puzzle until I go back and finish the other ones still in progress. But maybe by the
time you read this, it will be long after I finish all the puzzles for the season! As always, Day 25 only has one
puzzle to solve; part 2 is a freebie after accumulating the other 49 stars. 

--- 

## Part One

In this puzzle, we are given funny numbers in a syntax called "snafu," which we must decimalize, add together, and then
convert back into snafu format. Let's start with the simpler work of implementing `snafu->decimal`.

### Converting snafu to decimal

```clojure
(def powers-of-five (map #(Math/pow 5 %) (range)))
(def snafu-charmap {\2 2, \1 1, \0 0, \- -1, \= -2})

(defn snafu->decimal [snafu]
  (reduce + (map * (map snafu-charmap (reverse snafu)) powers-of-five)))
```

First, `powers-of-five` is a (you guessed it) infinite sequence of the powers of five. Using the infinite sequence
`(range)`, we use the Java function `Math.pow` to return every power of five, starting with `5^0` or `1`. We'll use
this sequence several times.

Then we create a `snafu-charmap`, which maps the five snafu characters to their decimal equivalents, from -2 to 2.

Finally, `snafu-decimal` takes in a snafu string, which we'll manipulate as a sequence of characters. First we reverse
them, so that the least significant digit is first, as that matches the order of values in `powers-of-five`. Then we
map each reversed snafu character to its decimal value from the `snafu-charmap`, multiply it by its power of five, and
then add together the matching digits. This tells us the complete decimal value.

### Converting decimal to snafu

Next, we need to figure out how to convert a decimal into a snafu character, and to do this I had to manually map out
a few values. If we look at the all the snafu values that can be represented by a single character (assuming only
positive numbers), it ranges from 1 (`1`) to 2 (`1=`). Two digits can capture from 3 (`1=`) to 12 (`22`). Three digits
can capture from 13 (`1==`) to 62 (`222`). We also know, from looking at the sample inputs, that the first digit is
always either a 1 or a 2, so that will help.

Knowing this information, we'll take the following approach. First, we'll determine how many snafu digits it will take
to represent the decimal number. The for each snafu digit, from most to least significant, we'll pick the value that's
closest the target number we're trying to reach, subtract that value from the target, and move on to the next digit.
This will make more sense as we do it.

Let's figure out how many snafu digits it will take to represent the decimal number.

```clojure
(def min-snafus-by-digit
  (letfn [(next-min [acc [x & xs]] (lazy-seq (cons (- x acc)
                                                   (next-min (+ acc x x) xs))))]
    (next-min 0 powers-of-five)))

(defn num-digits-in-snafu [n]
  (count (take-while (partial >= n) min-snafus-by-digit)))

```

`min-snafus-by-digit` is an infinite sequence of the smallest number that can be represented by that number of snafu
digits.  Another infinite sequence? This is getting silly now. We saw from the examples for 3, 13, and 63 - the
smallest numbers that can be represented for two, three, or four snafu digits - the value is always a `1` followed by
remaining `=`, which corresponds to 1 times the appropriate power of five, minus the sum of 2 times each of the
previous powers of five. Unlike `powers-of-five`, this sequence depends on a recursive call; I didn't want to force it
to be a function, since a `def` seems cleaner, which is why I implemented the `def` with an internal function that gets
lazily called.

`num-digits-in-snafu` finds how many `min-snafus-by-digit` are less than or equal to the target, as this will determine
how many powers of five we'll need to represent the number, in just a moment.

We're just about ready to create `decimal->snafu`.

```clojure
(defn num-snafus [five n]
  (->> (range 2 -3 -1)
       (map (fn [multiple] [(abs (- n (* multiple five))) multiple]))
       (sort-by first)
       first
       second))

(defn decimal->snafu [n]
  (let [charmap (set/map-invert snafu-charmap)]
    (first (reduce (fn [[result remaining] five] (let [digit-nums (num-snafus five remaining)]
                                                   [(str result (charmap digit-nums))
                                                    (- remaining (* digit-nums five))]))
                   ["" n]
                   (reverse (take (num-digits-in-snafu n) powers-of-five))))))
```

Ok, let's start with `num-snafus`. This function takes in a power of the most significant five and the target number
`n` we want to represent as a snafu digit. Because a snafu digit can be a -1 or -2, we cannot simply look for the least
number of `five` that fits into `n`; rather, we must look for the best/closest fit. Therefore, we start with the
numbers from 2 to -2 (I suppose I could have used `[2 1 0 -1 -2]` instead of `(range 2 -3 -1)` but here we are). Then
for each possible number which represents the snafu digit, we map it to a tuple of its distance from the target and its
actual value; the distance comes from multiply it by the fives digit, and calculating the absolute value of the
difference from the target `n`. `(sort-by first)` will fine the one with the smallest difference (best fit), and then
`first` and `second` strips out the first tuple and then the multiple itself. So the result is the function returning
a value from -2 to 2, inclusive.

Finally, there's `decimal->snafu`. We start by inverting the `snafu-charmap` function into a map from the decimal value
from `num-snafus` into the target snafu character; I still don't know why this function on maps appears in the
`clojure.set` namespace. Then we use `reduce` to construct the output character by character. The input collection is
the sequence of powers of five from most to least significance; we get this by taking the correct number of values
based on `num-digits-in-snafu`, and then reversing the result. The accumulation will be a simple vector of the 
building snafu string and the remaining numeric value to calculate. Then for each fives digit, we determine the number
of snafus for that digit, append the reversed character map representation for that number into the accumulated string,
subtract the product of the five and the number of fives from the remaining accumulation, noting that the number could
actually _increase_ if `digit-nums` is negative. Finally, once the `reduce` is done, we pull out the resulting string
with `first`.

Then we wrap it all up with the `part1` function.

```clojure
(defn part1 [input]
  (decimal->snafu (transduce (map snafu->decimal) + (str/split-lines input))))
```

Why not wrap up the final puzzle with a `transduce`, to bring the whole season to a nice close. We transduce over each
line of input, transforming with the `snafu->decimal` function, using `+` as the reducer, and then call
`decimal->snafu` on the resulting accumulated value to get our answer.

That's it! Once I finish the previous puzzles, that'll be a wrap for the season.
