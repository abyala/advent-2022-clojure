# Day 5: Supply Stacks

* [Problem statement](https://adventofcode.com/2022/day/5)
* [Solution code](https://github.com/abyala/advent-2022-clojure/blob/master/src/advent_2022_clojure/day05.clj)

---

## Intro

The way I solve Advent problems is fairly consistent - I like to parse the input into a format that makes sense for me
to manipulate, and then I go about solving the actual puzzle. For puzzles like this one, it means that I often spend as
much time parsing than doing anything else, even if it can be more efficient to just use the data as it comes in.
This is one of those days where it feels like my solution could have been shorter if I didn't do up-front parsing.
Oh well!

---

## Part One

We're given a somewhat complex input string this time, representing a number of named stacks, a blank line, and then a
set of instructions on how to move boxes from one stack to another. Based on the input I saw, I noticed that every box
and stack number is a single character; actually, the stack names are just incrementing numbers starting from 1, but I
didn't use that information very much.

### Parsing

So let's start with parsing the crane and its stack first, and then we'll move
on to the instructions.

```clojure
(defn parse-crane-line [line]
  (mapv (comp #(when (not= % \space) %) second)
        (partition-all 4 line)))

(defn parse-crane [s]
  (let [parsed (map parse-crane-line s)
        rows (butlast parsed)
        names (last parsed)]
    (reduce (fn [acc idx]
              (assoc acc (get names idx)
                         (keep identity (map #(get % idx) rows))))
            {}
            (range (count names)))))
```

Let's take these functions one at a time.

`parse-crane-line` takes a single line of text, which the test input shows can look like `    [D]    ` or `[Z] [M] [P]`.
I bank on the fact that every fourth character, starting with the second, is either a space (blank) or the name of the
crate in the stack. The `(partition-all 4 line)` returns a sequence of character lists up to 4 characters. We can then
`mapv` over this sequence, first pulling out the second character, and then returning either it or a `nil` if the value
is a space.

Then we can move on to the `parse-crane` function. After mapping each line of the first part of our input to
`parse-crane-line`, we'll separate out the last line using `butlast`, since it represents the names of the stacks and
not any of its crates. Then we'll want to return a crane, for which we'll use a map. The keys will be the name of the
stack (which we happen to know is a numeric character), and the values will be Clojure lists, since lists make
terrific stacks. The `reduce` function will look over the index of values in the `names` vector. To associate it into
the resulting crane map, we'll need to look at all rows of the parsed input, and grab the `nth` value out of it. Any
`nil` value will appear at the top of the rows, so calling `(keep identity)` will remove all of the `nil`s and give us
what we're looking for.

Parsing each instruction line is much simpler.

```clojure
(defn parse-instruction [s]
  (let [[quantity from to] (re-seq #"\d+" s)]
    {:quantity (parse-long quantity) :from (first from) :to (first to)}))
```
This is the one place I depended on knowing each stack was a single-digit number. We already saw `re-seq` in the
[day 4 puzzle](https://github.com/abyala/advent-2022-clojure/blob/main/docs/day04.md), and here we use it again to pull
out the number of boxes to move, and the names from the `from` and `to` stacks. The function will return a map with
keys `:quantity`, `:from`, and `:to`. The quantity must be numeric, so we call `parse-long`, while the other two must
be characters, so we call `first` on the parsed string data.

Finally, we'll parse the entire input using the `parse` function. This will return a simple map.

```clojure
(defn parse [input]
  (let [[crane-str instruction-str] (utils/split-blank-line-groups input)]
    {:crane        (parse-crane crane-str)
     :instructions (map parse-instruction instruction-str)}))
```

### Problem logic

Well now the problem is actually rather simple. To start, let's create `apply-instruction` to move boxes from an
existing parsed crane.

```clojure
(defn apply-instruction [crane {:keys [from to quantity]}]
  (let [moving (take quantity (get crane from))]
    (-> crane
        (update to #(apply conj % moving))
        (update from #(drop quantity %)))))
```

After destructuring the instruction into its components `from`, `to`, and `quantity`, we start by figuring out which
boxes we're moving. `(get crane from)` returns the sequence/stack for the stack labeled `from`, from which we call
`(take quantity)`. Remember that data structures in Clojure are immutable, so this doesn't actually alter the sequence
or the crane itself in any way. To do that, we'll need to do two `updates` to the crane, which one again doesn't
actually affect the crane itself, but in instead returns a crane with the updates applied to it.

`(update crane to #(apply conj % moving))` says we're going to apply the anonymous function to the value in the crane
at key `to`, meaning the destination sequence/stack. Calling `(conj list v)` adds the value `v` to the front of list
`list`, so `(apply conj % moving)` calls `conj` on each value of `moving` onto the current list. Then once that's done,
`update crane from #(drop quantity %))` removes the first `quantity` values from the original stack. Thus, we've moved
all of the values.

Now we need to run all of the instructions, and unsurprisingly we'll see `reduce` come into the picture, running
`apply-instruction` over each parsed instruction, starting with the initial crane.

```clojure
(defn apply-all-instructions [crane instructions]
  (reduce apply-instruction crane instructions))
```

Now before we finish, we'll need to report on all of the "top" crates in the crane, so let's whip that together
quickly.

```clojure
(defn top-of-crane [crane]
  (->> crane vals (map first) (apply str)))
```

This function says to start with the crane (a map), extract out the values, take the first (top) value from each
sequence, and then stringify those characters using `(apply str)`. I'll be honest, I was surprised that the values came
out of the map in key order, but I won't complain!

Finally, we implement the `part1` function, where we parse the input, call `apply-all-instructions`, and then
`top-of-crane` to wrap it up.

```clojure
(defn part1 [input]
  (let [{:keys [crane instructions]} (parse input)]
    (top-of-crane (apply-all-instructions crane instructions))))
```

See? The puzzle wasn't that bad once we finished parsing the input.

---

## Part Two

Well part two is actually quite simple now. The problem states that the fancy new crane can pick up multiple crates at
once, and place them back down in the order they came in. This just says to me that if we picked up the first three
crates from a stack holding values `(A B C D E)` onto a stack containing `(x Y Z)`, then the part 1 crane would create
a new stack `(C B A X Y Z)` while the part 2 crane would create `(A B C X Y Z)`. In other words, if we simply reverse
the values taken from the source stack before placing them on the target stack, they'll show up in order.

So let's look at the `apply-instruction` function, and add in a new argument called `one-at-a-time?`, which will be 
`true` for part 1 and `false` for part 2.

```clojure
(defn apply-instruction [one-at-a-time? crane {:keys [from to quantity]}]
  (let [crane-fn (if one-at-a-time? identity reverse)
        moving (crane-fn (take quantity (get crane from)))]
    (-> crane
        (update to #(apply conj % moving))
        (update from #(drop quantity %)))))
```

Note the new `crane-fn` binding we create and apply to the `(take quantity (get crane from))` call. For part 1, we
don't need to change those crates at all, so `identity` makes has no impact on it. For part 2, the `crane-fn` will be
`reverse` to swap the order of boxes. Everything else is unchanged.

To wrap this up, we just need to pass the values of `one-at-a-time?` in through the `solve` and `apply-all-instructions`
functions to get to our answer!

```clojure
(defn apply-all-instructions [one-at-a-time? crane instructions]
  (reduce (partial apply-instruction one-at-a-time?) crane instructions))

(defn solve [one-at-a-time? input]
  (let [{:keys [crane instructions]} (parse input)]
    (top-of-crane (apply-all-instructions one-at-a-time? crane instructions))))

(defn part1 [input] (solve true input))
(defn part2 [input] (solve false input))
```

The only interesting code here is the `apply-all-instructions` function. The first argument of `reduce` takes in a
function with two arguments (accumulator and next value), but we're now calling the `apply-instruction` function which
takes in three arguments. Instead of using `(fn [acc v] (apply-instruction one-at-a-time? acc v))`, we can use a
partial function to represent part of the function call; `(partial apply-instruction one-at-a-time?)` effectively locks
down the first argument of the `apply-instruction` call. With only two arguments remaining, this partial function fits
into the contract of the first argument of `reduce`, and hence why we get a clean one-line `reduce` call.

So even with both parts 1 and 2 defined, the parsing logic was still bigger than the problem. It just goes to show the
value of preparing your data before trying to do more complex algorithms. And in Clojure, where everything is data,
it's really simply to work on the business logic once you have parsing out of the way.
