# Day 20: Grove Positioning System

* [Problem statement](https://adventofcode.com/2022/day/20)
* [Solution code](https://github.com/abyala/advent-2022-clojure/blob/master/src/advent_2022_clojure/day20.clj)

---

## Intro

I'm starting to lose my mojo with Advent this year, as I've fallen behind on the previous 4 puzzles, having found them
to be a little irritating. So this one works, even if it's slow, and I'm not likely to improve it.

---

## Part One

We are given a list of numbers that exist in a circular list, and we are instructed to move each number a number of
spaces based on its value. The trick is that the numbers move in the order in which they originally appeared in the
list, so we have to remember which numbers appeared where when we started the program. Additionally, I verified that
there _are_ duplicate values, so we cannot just map each index to its original value.

I think this puzzle might have been very easy using OO, since I would probalby have created a linked list of nodes, and
another list of each node in order. That way, I would have just iterated over the second list to find each node, and
move it around the list as necessary. I may try implementing something non-OO that's similar to this using maps, but
not today. My two solutions involved using vectors with `subvec` and lists with `cycle`, and I found the former to be
faster, so that's what I will present.

First, we parse the input into a vector of `[[idx0 val0], [idx1 val1]...]`.

```clojure
(defn parse-input [input]
  (vec (map-indexed #(vector %1 (parse-long %2)) (str/split-lines input))))
```

We use `map-indexed` over the split lines of data, turning each value into a vector of `%1` (the index) and
`(parse-long %2)` (the numeric string), which returns a sequence of vectors. Then we convert it all into a vector of
vectors by wrapping it all in `vec`.

Next, showing the weakness of the non-OO solution, I created `index-of-original-index`, which scans all of the vectors
to find the one whose first value matches the index of the original list. Note that this means the algorithm is of
complexity `O(n^2)`, which is why it's not the fastest. So... as I'm writing this, I'm likely to do a third
implementation soon!

```clojure
(defn index-of-original-index [idx nums]
  (index-of-first #(= (first %) idx) nums))
```

This function leverages the `index-of-first` function we created in the Day 6 puzzle, where we return the index of the
first (and only) vector whose first element is the original index we're seeking.

The bulk of the work occurs in the `rotate-at-index` function, which admittedly involved my messing with offsets until
I was able to get the data to look correct. The idea is that, given the index of the _current_ node to move, and the
vector of numbers, return the new vector with that node properly moved.

```clojure
(defn rotate-at-index [idx nums]
  (let [[_ rotation :as node] (nums idx)
        idx' (mod (+ idx rotation) (dec (count nums)))]
    (cond (= idx idx') nums
          (< idx' idx) (reduce into (subvec nums 0 idx') [[node]
                                                          (subvec nums idx' idx)
                                                          (subvec nums (inc idx))])
          :else (reduce into (subvec nums 0 idx) [(subvec nums (inc idx) (inc idx'))
                                                  [node]
                                                  (subvec nums (inc idx'))]))))
```

First of all, we need to calculate the target index of the node, represented as `idx'`. To do this, we grab the
rotation amount (second value in that node's vector), add it to the current index, and then mod that by _one less than_
the number of values in the vector. I got tripped on this for a while, because we need to remove the node from the
vector (leaving the vector with its original size - 1), then rotate around, and then put it back in.

After that, it's a matter of constructing the new vector. If the index doesn't move, just return the current `nums`
vector. If the index moves to the left `(< idx' idx)`, then we take all of the vector values leading up to where the
new node goes, then inject the node, then the remaining values leading up to where the moving node used to reside, and
finally the values after the position of the old node. We do similar work if the node moves to the right - grab all of
the values up to the old value of the node, then the remaining values leading up to the new position of the node, then
the node itself, and finally the trailing values.

Ok, we're ready for the `mix` function, which takes in the vector of nodes and rotates each value in its original
order.

```clojure
(defn mix [nums]
  (reduce (fn [acc idx] (rotate-at-index (index-of-original-index idx acc) acc))
          nums
          (range 0 (count nums))))
```

Easy enough - looping through the original indexes, from `(range 0 (count nums))`, we find the index of the original
index (the sad little `n^2` function), then rotate the accumulated list at that index using `rotate-at-index`.

Finally, we're ready for `part1`.

```clojure
(defn part1 [input]
  (let [nums (->> input parse-input mix (map second))
        v-cycle (drop-while (complement zero?) (cycle nums))]
    (transduce (map #(first (drop % v-cycle))) + [1000 2000 3000])))
```

To start, we'll parse the input, `mix` the nodes, and then extract out the node values using `(map second)` to get rid
of the indexes. Then we need to reset the list to the value starting after the zero, so we'll make a repeating loop of
the values with `cycle`, dropping everything until we find the one matching a zero. Finally, with this new infinite
sequence, we'll pull out the values at offset 1000, 2000, and 3000 and `transduce` them using `+` to get our answer.

---

## Part Two

We actually have everything we need for part 2, even if it's a little messy.

```clojure
(defn part2 [input]
  (let [nums (mapv #(update % 1 * 811589153) (parse-input input))
        v-cycle (->> (iterate mix nums)
                     (drop 10)
                     first
                     (map second)
                     cycle
                     (drop-while (complement zero?)))]
    (transduce (map (comp first #(drop % v-cycle))) + [1000 2000 3000])))
```

To start off, after parsing the input, we need to multiply the value in each node by the decryption key of 811589153.
Because we can treat a vector as a map of indexes to values, we can use `mapv` and the `update` function, where `1` is
the key of the vector (map) whose value we multiply by the decryption key. So the numbers within
`(mapv #(update % 1 * 811589153) (parse-input input))` look a little funny, but the 1 is the index and the other number
is just a multiplication argument.

Then we make `v-cycle`, the infinite cycle of values, by mixing the values 10 times, and doing everything we did in
part 1 - strip away the indexes using `(map second)`, make an infinite cycle, drop until we start with a zero, and then
add together the 1000th, 2000th, and 3000th values.

No surprise - we can easily extract out a shared `solve` function, which takes in the decryption key to apply and the
number of mixes to perform.

```clojure
(defn solve [decryption-key num-mixes input]
  (->> (parse-input input)
       (mapv #(update % 1 * decryption-key))
       (iterate mix)
       (drop num-mixes)
       (first)
       (map second)
       (cycle)
       (drop-while (complement zero?))
       (rest)
       (partition 1000)
       (map last)
       (take 3)
       (reduce +)))

(defn part1 [input] (solve 1 1 input))
(defn part2 [input] (solve 811589153 10 input))
```

I did decide to change up the code every so slightly. After applying the decryption key, mixing, and getting to the
cycle of values, I chose not to grab the `nth` values in the sequence three times, as that would involve iterating
through the same data multiple times. So instead, I dropped the zero value and created partitions of 1000 values, using
`(map last)` to return a sequence of every 1000 values after the zero. Then it was just a matter of taking the first 3
and adding them together to get our answer.

So, yeah. The solution isn't lightning fast due to the constant scanning of the vector, and I'll bet I could refactor
the data structure to be a map of the following to be lightning fast:

```clojure
{:indexes {0 current-idx0, 1 current-idx1...},
 :nodes [v0, v1, v2...]}
```

I might implement that and update here accordingly.