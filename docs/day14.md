# Day 14: Regolith Reservoir

* [Problem statement](https://adventofcode.com/2022/day/14)
* [Solution code](https://github.com/abyala/advent-2022-clojure/blob/master/src/advent_2022_clojure/day14.clj)

---

## Intro

We're on a roll here, with another fun puzzle that reminded me of one from a previous year!

---

## Part One

We're given a description of rock formations within a cave, and we need to track how sand flows into the cave itself.
When parsing the input, we want to represent a cave as a map with keys `:points` and `:floor`, where the former is a 
set of all `[x y]` coordinates that have been filled by either rocks or sand, and the latter is the height of the
first row underneath the lowest rock formation. The latter will make more sense for part 2, but we can imagine that if
we want to know when the sand starts falling into a bottomless cavern, it's essentially the same as the sand falling
onto the floor at a point lower than what we can measure. I think there's a metaphor here about the existence of God
if you squint a little bit.

### Parsing

We'll start with a simple function to parse a single point in the input, expressed as the string `"498,4"` and which
we want to convert to the numeric vector `[498 4]`.

```clojure
(defn parse-point [s] (mapv parse-long (str/split s #",")))
```

We did look yesterday at using `edn/read-string`, which we could also theoretically do here, but we'd first need to
turn that string of `"498,4"` into one that looks like a string of the vector `"[498,4]"`. So this is another
implementation, but I don't like it.

```clojure
; This seems a poor use of edn/read-string. Not using it.
(defn parse-point [s] (edn/read-string (str "[" s "]")))
```

Next we need to know how to gather all of the inclusive points between two points that are either horizontally or
vertically aligned. I tried a few ways of doing this, but this looks the least "clever" and the easiest to read to me:

```clojure
(defn line-of-points [[x1 y1 :as p1] [x2 y2 :as p2]]
  (case (map (comp signum -) p1 p2)
    [0 0] [p1]
    [0 1] (map vector (repeat x1) (range y2 (inc y1)))
    [1 0] (map vector (range x2 (inc x1)) (repeat y1))
    (recur p2 p1)))
```

Given two destructured points, we'll map each pair of `x` and `y` coordinates to the `signum` of their differences,
such that we'll get a vector of `[dx xy]` for the sign differences between each ordinate. Then we look at the vectors.
If somehow the two values are equal, that means the input had `[x y] -> [x y]`, which I don't think happens, but we
handle it regardless. In the other scenarios, we hold either the `x` or `y` ordinate fixed with `(repeat x1`) or
`(repeat y1`), and do a `(map vector)` of that sequence against the range of other values. To avoid having to handle
whether the points were increasing or decreasing, I just called `(recur p2 p1)` to cover the cases of `[0 -1]` and
`[-1 0]`.

Next, we'll define `rock-points`, which will convert a line of rock point progressions to a set of all points included
by that rock formation.

```clojure
(defn rock-points [s]
  (->> (re-seq #"\d+,\d+" s)
       (map parse-point)
       (partition 2 1)
       (mapcat (partial apply line-of-points))
       set))
```

The regex `#"\d+,\d+"` splits out the line into a sequence of `x,y` points, which we then map to their `[x y]` numeric
vectors using `parse-point`. `(partition 2 1)` groups the values into overlapping pairs, to which we call 
`line-of-points` to get their individual points. Finally, we call `set` to remove the duplicates that will occur at
the corners of the rock formations.

Finally, we're ready to parse the data. I implemented this solution a few ways, but for the one I'm going to present
now, I'm going to implement a simple `parse-cave` function, which takes in the input string and returns a single set
of the rock points.

```clojure
(defn parse-cave [input]
  (transduce (map rock-points) set/union #{} (str/split-lines input)))
```

No big shocker here, we're transducing. Given the lines of input, we transform each one using `rock-points`, and 
union them together into an empty set. On to the business of dropping sand into caves. And while not strictly related
to parsing, for some solutions (not shown) I implemented this within the `parse-cave`, so here's a helper function
called `cave-floor`, which returns the `y` value just past the lowest depth in the cave. Because depth increases as 
we go deeper, this is a positive number.

```clojure
(defn cave-floor [points]
  (->> points (map second) (reduce max) inc))
```

### Problem logic

Our goal now is to implement a function called `next-sand`, which takes in the cave floor and all of the points
currently occupied by either rocks or sand (it doesn't matter which one anymore), and returns the point where the next
grain of sand will settle. For this to work, we're also implelmenting `possible-next-spaces`, which returns the
coordinates of the points below, down-left, and down-right from the coordinates passed in.

```clojure
(def cave-entrance [500 0])

(defn possible-next-spaces [[x y]]
  (map vector [x (dec x) (inc x)] (repeat (inc y))))

(defn next-sand [floor points]
  (when-not (points cave-entrance)
    (loop [p cave-entrance]
      (if (= (second p) floor)
        p
        (if-let [p' (->> p possible-next-spaces (remove points) first)]
          (recur p')
          p)))))
```

`possible-next-spaces` isn't too complicated. We're using `(map vector)` against two collections: the `x` ordinates in
preferred order, and an infinite sequence of the value beyond the `y` ordinate. So for input `[500 0]`, we will receive
`([499 1] [500 1] [501 1])`.

`next-sand` has a few minor tricks, but it's still intuitive. First off, if the `cave-entrance`, a constant tied to
`[500 0]` is already blocked up by one of the points, the sand cannot settle. We don't need to explicitly return `nil`,
since `when` is just an `if` function without the possibility of an `else` clause. Then we begin a `loop-recur` that
starts from the cave entrance. If at any point, the depth hits the floor, the sand has to settle, so just return `p`.
If not, check to see if any of the available spaces aren't currently occupied, by calling `(remove points spaces)`.
If so, then recurse back in to learn if that point is an intermediate point for the sand or its final settling space.
If there are no unoccupied available spaces below the current point, then this is the settling point.

Now comes the fun part. Texting with [Todd Ginsberg](https://github.com/tginsberg/advent-2022-kotlin) while waiting for
my lunch before doing this write-up, I heard his clever idea to have his code return a sequence of points representing
where each new grain of sand will settle. My original solution returned a revised state of all points after the sand
settled, but I liked his approach so I refactored mine. I think there are some cool things to show in the `sand-seq`
function.

```clojure
(defn sand-seq [cave]
  (letfn [(find-next [floor points] (when-some [sand (next-sand floor points)]
                                      (lazy-seq (cons sand (find-next floor (conj points sand))))))]
    (find-next (cave-floor cave) cave)))
```

I decided to define this function to take in the `cave` we parsed out back in `parse-cave`, but to use a hidden function
defined using `letfn` instead of a top-level `defn` or `defn-` call. The impact of this is that the hidden function,
which I called `find-next`, is not available to any other caller within or outside the namespace. This hidden function
takes in the floor of the cave, and _all_ points identified to be occupied, whether by rocks or by sand. `find-next`
calls `next-sand` to see if there is another point available for sand to settle, and if so, returns a lazy sequence
with the coordinates of this sand grain, followed by the rest of the lazy sequence that comes from recursively calling
back into `find-next` again. The recursed call combines the previously known occupied points with the new point using
`(conj points sand)`. Finally, the outer `sand-seq` function just calls the inner `find-next` with the calculated
`(cave-floor cave)` and all of the rock points of the cave.

I like this solution for multiple reasons. First off, a lazy sequence is more data oriented than making the caller
invoke `(iterate next-state-fn)`. It also reinforces the use of laziness wherever feasible, and in this case it's
perfectly reasonable to calculate the next sand point on-demand. Finally, this solution completely abstracts away all
of the interim state, namely the calculated floor and all of the rocks and sand already accumulated.

We're now ready for `part1`.

```clojure
(defn part1 [input]
  (let [cave (parse-cave input)
        floor (cave-floor cave)]
    (->> (sand-seq cave)
         (take-while (fn [[_ y]] (< y floor)))
         count)))
```

All we do here is parse the cave and calculate the floor (so we only have to do it once), work through the `sand-seq`
points, keeping only the values that don't hit the floor. From that sequence of points still within the cave, we just
count them up.

---

## Part Two

Well there's not much to do here, since the code is already written for us.

```clojure
(defn part2 [input]
  (->> (parse-cave input)
       sand-seq
       (take-while some?)
       count))
```

This code doesn't care about the floor anymore; we just need to pull values until we fill up the cave entrance. Since
both the `next-sand` and `sand-seq` functions return `nil` when there's no new point for the sand, we can just call
`(take-while some?)` to find all non-`nil` values, and then count them up.

---

## Refactoring (I don't like this)

I usually like to make a unified `solve` function, but I don't like it in this case, so I don't actually use it in my
solution. The issue is that `part1` needs to consider both the floor of the cave and the points coming in, while `part2`
only cares that points are being produced. Therefore, the `take-fn` helper function that the unified `solve` function
needs has to expose two arguments, and I just don't love how it all looks.

```clojure
(defn solve [take-fn input]
  (let [cave (parse-cave input)
        floor (cave-floor cave)]
    (->> (sand-seq cave)
         (take-while (fn [p] (take-fn floor p)))
         count)))

(defn part1 [input] (solve (fn [floor [_ y]] (< y floor)) input))
(defn part2 [input] (solve (fn [_ p] (some? p)) input))
```

So it works... yeah. I prefer the original solution, so that's what I'm about to commit. Onward!
