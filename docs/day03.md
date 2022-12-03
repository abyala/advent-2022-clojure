# Day 3: Rucksack Reorganization

* [Problem statement](https://adventofcode.com/2022/day/3)
* [Solution code](https://github.com/abyala/advent-2022-clojure/blob/master/src/advent_2022_clojure/day03.clj)

---

## Part One

Today's problem deals with a little string manipulation and some set functions.

In the first puzzle we need to look through each line of input and find which single character appears in both
halves of the string, and the transform each character. To start off, we'll need a way to split a string in
half. I found two acceptable ways of doing this. First, we can determine the `pivot`, being the index of the
midpoint of the string, and then return a vector of the two relevant substrings. Second, we can use the `split-at`
function, which is just a combination of `(take n s)` and `(drop n s)`. Note that the first function returns a vector of
two strings, while the second returns a vector of character sequences. The former output looks a little more
predictable, but the latter works just as well and is easier to read, so let's assume we use that one.

```clojure
; Returns a vector of two strings
(defn string-halves [s]
  (let [pivot (/ (count s) 2)]
    [(subs s 0 pivot) (subs s pivot)]))

; Returns a vector of two character sequences - I'll keep this one
(defn string-halves [s]
  (split-at (/ (count s) 2) s))
```

Now assuming that we've got our vector of strings, we can implement the `find-duplicate` function. This simply converts
each string into a set of characters, seeks out the intersection of these character sets, and returns the first 
(and hopefully only!) character found.

```clojure
(defn find-duplicate [strings]
  (->> (map set strings)
       (apply set/intersection)
       first))
```

Each character has a `priority` value, which is 1-26 for lowercase letters and 27-52 for capital ones. Note that we
would normally expect the capital letters to appear before the lowercase ones, so we need to play with the values to
find which numbers to subtract from the ASCII value of each character. And to solve the puzzle, we'll continue the 
trend of using transducers, feeding in the line of strings, transforming each string by mapping it to its halves of 
characters, then its duplicate character and its priority value, and finally reducing over the sum of priority values.

```clojure
(defn priority [^Character c]
  (- (int c) (if (Character/isUpperCase c) 38 96)))

(defn part1 [input]
  (transduce (map (comp priority find-duplicate string-halves)) + (str/split-lines input)))
```

---

## Part Two

For this part, we need to find the one character that appears in each triple of lines in the input, and then
sum that character's priority. This looks very similar to what part 1 is all about - we find groups of strings (halves
of each line in part 1, and groups of three in part 2), before looking for the unique characters and calculating their
priorities. The only difference in the two solutions is the grouping function, so let's go straight to the refactoring
we'd normally do later on.

We know that our solution will need to be told how to group the sequence of split lines, so the `solve` function will
need to receive its `grouping-fn` to apply to the lines of text. Then as with `part1`, this is just a simple transducer
transforms each line group by finding the duplicate and determining its priority.

```clojure
(defn solve [grouping-fn input]
  (transduce (map (comp priority find-duplicate)) + (grouping-fn (str/split-lines input))))
```

To leverage this function, we simply have to supply the grouping functions, here shown as partial functions.  

```clojure
(defn part1 [input] (solve (partial map string-halves) input))
(defn part2 [input] (solve (partial partition 3) input))
```
