# Day 13: Distress Signal

* [Problem statement](https://adventofcode.com/2022/day/13)
* [Solution code](https://github.com/abyala/advent-2022-clojure/blob/master/src/advent_2022_clojure/day13.clj)

---

A large portion of this problem was almost laughably simple since I'm using Clojure. Each line of input is valid
[edn (Extensible Data Notation)](https://github.com/edn-format/edn), which Clojure is all too happy to read directly
into normal data structures!

---

## Part One

The bulk of Part 1 involves comparing two packets to see if they are in order. As with every Advent problem, let's
start with our parsing logic.

```clojure
(edn/read-string line)
```

Yep, that's it. We could use either the `clojure.core/read-string` or `clojure.edn/read-string` functions to parse
each line; both work just fine, but the latter is safer in that it cannot execute code, while the former can. I know
that the input data is safe for Advent, but there's no reason _not_ to use the safer option.

Whew! That was rough. Let's move on.

The bulk of the work is going to come down to the `correct-order?` function, which will take in two arities:

```clojure
(defn correct-order? ([left right] ...)
                     ([left right address] ...))
```

I implemented this a couple of ways, and found that I was happiest when I (mostly) didn't modify the left and right
packets, instead passing along an `address` vector that represented the location within the packets we would access to
compare elements. Before showing the implementation, I want to introduce four helper functions:

```clojure
(defn move-up [address] (subvec address 0 (dec (count address))))
(defn move-down [address] (conj address 0))
(defn move-right [address] (update address (dec (count address)) inc))
(defn vectorize [packet address] (update-in packet address vector))
```

Moving "down" means to drill into a sub-vector, where the `get-in` function just adds a zero to its end to represent the
first index in the subvector. Likewise, moving "up" means removing the last term of the address vector. Moving "right"
means to go to the next value at the current level, which we do by calling `inc` on the last index in the address.
Finally, the `vectorize` is the only function that will mutate a packet (or make a copy of a mutated packet... this is
Clojure after all and we don't change our data structures!). The function modifies the value at an address of the
packet to convert itself from a scalar value to a vector of that scalar.

With that out of the way, let's write out `correct-order?`.

```clojure
(defn correct-order?
  ([left right] (correct-order? left right [0]))
  ([left right address]
   (let [[v1 v2 :as values] (map #(get-in % address) [left right])]
     (cond
       (every? nil? values) (recur left right (-> address move-up move-right))
       (nil? v1) true
       (nil? v2) false
       (every? number? values) (case (u/signum (- v1 v2))
                                 -1 true
                                 1 false
                                 (recur left right (move-right address)))
       (every? vector? values) (recur left right (move-down address))
       (vector? v1) (recur left (vectorize right address) address)
       :else (recur (vectorize left address) right address)))))
```

I'm sure I could get rid of a condition or two, but I'm ok with what I see for now. We start off with the 2-arity
function that calls into the 3-arity function with the starting root address of `[0]`. Then we pull out the values from
the `left` and `right` packets at the correct address, binding them both individually to `v1` and `v2`, and as their
outgoing sequence as `values`. Then we have one giant conditional to match against the proper scenario for the input.
* If we're looking at two `nil` values, then we were in a subvector such that both sides were identical. So we need to
  move back "up" out of the subvector, and to the "right" in the parent.
* If `v1` is `nil`, then the left vector ran out first, which means it's in order.
* If `v2` is `nil`, then the second vector ran out first, which means it's out of order.
* If both values are numbers, then we compare them. When they're not equal, use the signum to determine whether
  they're in order. If the two values are equal, then we need to move "right" to do the next comparison in order.
* If both values are vectors, then we move "down" into them to inspect their values.
* Otherwise, either `v1` is a vector and `v2` is scalar, or vice versa. So vectorize the scalar and loop back into the
  function again.

Well that wasn't bad! Now it's time for `part1`, which of course we'll solve with `transduce` because that's what we
do.

```clojure
(defn part1 [input]
  (transduce (keep-indexed #(when (apply correct-order? %2) (inc %1)))
             +
             (u/split-blank-line-groups edn/read-string input)))
```

It's really simple. Split the input by blank line groups, passing in a transformation function `edn/read-string` to
apply to each line within the line groups. Then let the transformation function be a simple `keep-indexed`, which says
that when the pair of parsed lines are in order, keep their index (incremented because Advent lists are 1-indexed).
Finally, reduce with the `+` function to get to our answer.

---

## Part Two

Part two was a snap, given how we implemented `correct-order?`. We just have combine all of our non-blank lines
together to get a master list of packets, add two "divider packets," sort everything, and then multiply the indexes of
the divider packets (1-indexed again). Very simple.

To start off, let's implement a `sort-packets` function, which just calls `sort` with a comparator.

```clojure
(defn sort-packets [packets]
  (sort (fn [a b] (if (correct-order? a b) -1 1)) packets))
```

Then we can implement `part2` already!

```clojure
(defn part2 [input]
  (transduce (keep-indexed (fn [idx v] (when (divider-packets v) (inc idx))))
             *
             (->> (str/split-lines input)
                  (remove str/blank?)
                  (map edn/read-string)
                  (concat divider-packets)
                  sort-packets)))
```

Again we'll use a `transduce` function, but there's a bit more to do to the collection before we can run it through the
transformer. I don't know of any way to simplify this any better, but I'm going to ask on the Clojurian Slack for help,
because so far as I can tell, it would be possible to create a stateful transducer that could sort the values before
calling `keep-indexed`, but I don't see how that would be simpler than what I have.

So here, we prepare the input collection by splitting each line, removing the blanks, parsing them, adding in the two
`divider-packets`, and then sorting the whole bunch. Then we do another `keep-indexed` like we did in part 1, except
that we only want the indxes of the values that equal the `divider-packets`. Luckily, I implemented `divider-packets`
as a set, so we can look them up as a function again. Finally, after incrementing the indexes, we just multiply them
together in the reducer function.

I don't see a reason to try and combine `part1` and `part2` any further; they look pretty good to me as is.

---

## Refactoring

I thought I'd give a library a try this time around, since I've seen others use it in the past - 
[clojure.core/match](https://github.com/clojure/core.match/wiki). This adds additional pattern matching powers to
Clojure. The idea is that you give the matcher a vector of values to look at, and it'll look at the pattern of data
coming in to figure out which expression to run.

I do need to ask around for some help, because it seems to be just fine at reading literals, including class literals,
but while it can pattern match around a single argument's class, it struggled with two of them. I had to use the uglier
guard clause syntax to get it to work. Still, if I can figure out the trick to it, this is another reasonable way to
implement the `correct-order?` function. Without something cleaner, though, I think it's ugly so I won't be using it
just yet in my published code.

```clojure
; implementation referring to [clojure.core.match :as m]
(defn correct-order?
  ([left right] (correct-order? left right [0]))
  ([left right address]
   (let [[v1 v2] (map #(get-in % address) [left right])]
     (m/match [v1 v2]
              [nil nil] (recur left right (-> address move-up move-right))
              [nil _] true
              [_ nil] false
              [(_ :guard number?) (_ :guard number?)] (case (u/signum (- v1 v2))
                                                        -1 true
                                                        1 false
                                                        (recur left right (move-right address)))
              [(_ :guard vector?) (_ :guard vector?)] (recur left right (move-down address))
              [(_ :guard vector?) _] (recur left (vectorize right address) address)
              [_ (_ :guard vector?)] (recur (vectorize left address) right address)))))
```