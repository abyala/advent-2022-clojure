# Day 23: Unstable Diffusion

* [Problem statement](https://adventofcode.com/2022/day/23)
* [Solution code](https://github.com/abyala/advent-2022-clojure/blob/master/src/advent_2022_clojure/day23.clj)

---

## Part One

Given a map of elves in a grid, we need to make them move in a number of generations and see where they end up; Advent
often has Game Of Life puzzles, which I actually rather like, so this was a fun puzzle.

To start off, let's parse the input. As we have an infinite board and only care about the elves, we'll represent the
data as a simple set of coordinates where the elves are currently standing.

```clojure
(defn parse-elves [input]
  (->> (p/parse-to-char-coords input)
       (keep (fn [[coords v]] (when (= v \#) coords)))
       set))
```

We use the `parse-to-char-coords` utility function from the `advent-2022-clojure.point` namespace, giving us back a
sequence of `[coords v]` vectors, from which we keep the coordinates when the value is the `#` sign, and then turn the
resulting sequence into a set.

Next, I decided to represent each of the directional comparisons as a vector of three point vectors, where the first
point is the direction in which the elf will move if permitted. We have to remember that when parsing the data, the
top rows have the _lowest_ row numbers, so the point due north of point `[x y]` is at `[x (dec y)]`, which is common
for these puzzles. Rather that using some constants from a shared library, I defined the three points to the north as
`[[0 -1] [-1 -1] [1 -1]]`, where the first point of `[0 -1]` is where we'll go if all three points are empty.

```clojure
(def northward [[0 -1] [-1 -1] [1 -1]])
(def southward [[0 1] [-1 1] [1 1]])
(def westward [[-1 0] [-1 -1] [-1 1]])
(def eastward [[1 0] [1 -1] [1 1]])
(def direction-partitions (partition 4 1 (cycle [northward southward westward eastward])))
```

I do love a good infinite sequence, and `direction-partitions` is a way of representing the four directions that we
want to compare in order. By cycling `northward`, `southward`, `westward`, and `eastward`, we get an infinite sequence
of those vectors, and by calling `(partition 4 1)` we get an infinite sequence of the four directions, effectively
pushing the first direction to the end of the list with each grouping. Thus the first partition will be
`(northward southward westward eastward)`, the second will be `(southward westward eastward northward)`, and so on.

The next function is `propose-next-step`, which takes in the current location of all elves, the current direction
partition, and the elf in question. If the elf has any neighbors (isolated elves don't move), he looks for the first
open direction and proposes moving in that direction.

```clojure
(defn propose-next-step [elves directions elf]
  (when-some [[dir] (and (some elves (p/surrounding elf))
                         (first-when (fn [dirs] (not-any? #(elves (mapv + elf %)) dirs)) directions))]
    (mapv + elf dir)))
```

Let's first talk about the humble `and` function, and its superpower in this case. `and` returns a truthy answer if all
of its elements are truthy, or else the first falsey answer. This doesn't mean it returns `true` and `false`, but
rather the _first falsey or last truthy value_ in its arguments. So to be clear `(and 3 5)` returns `5`, while
`(and 3 nil 5 false)` returns `nil`. This works because `5` is truthy, and `nil` is falsey.

In our case, we want to pick the first direction from the direction partition based on the first available direction,
but only if the elf has any neighbors. We start with `(some elves (p/surrounding elf))` to map the `elf` to its 8
neighboring points, and checking to see if any of them match the `elves` set. If so, then `first-when` will identify
the first directional triple where `not-any?` says that there is no neighbor in any of those distances from the elf.
If either the `(some elves)` or the `(first-when)` fails, then the `and` will return a falsey value, and `when-some`
will return `nil`. But if both conditions are met, then `and` returns the directional triple back, being the last
argument to `and`, and then we immediately destructure out the first direction using the `[dir]` binding. Finally, if
there is a direction to apply, we add it to the current elf using `(mapv + elf dir)`.

Now it's time to play out a single round of elves, calling `move-elves` with the set of current elf positions and the
directional partitions to use for the round.

```clojure
(defn move-elves [elves directions]
  (let [state' (reduce (fn [{:keys [targeted blocked] :as acc} elf]
                         (if-let [elf' (propose-next-step elves directions elf)]
                           (cond
                             (blocked elf') (update acc :staying conj elf)
                             (targeted elf') (-> acc
                                                 (update :blocked conj elf')
                                                 (update :staying conj (targeted elf') elf)
                                                 (update :targeted dissoc elf'))
                             :else (update acc :targeted assoc elf' elf))
                           (update acc :staying conj elf)))
                       {:staying #{}, :targeted {}, :blocked #{}}
                       elves)]
    (into (:staying state') (keys (:targeted state')))))
```

The goal here is to call `reduce` over all the elves, resulting in a map of the elves not moving `(:staying)` and the
ones that are moving `(:targeted)`. We'll then combine those elves together to get the new state.

In this `reduce`, we'll start with a base map of two empty sets called `:staying` and `:blocked`, and one map of
`:targeted`. We'll look at each elf, checking to see if there is a proposed next step; if not, then the elf has nowhere
to go, so it joins the `:staying` collection using `(update acc :staying conj elf)`. If there is a proposed place to
go, then whether it moves or not depends on the other elves. If the space is all clear, we add the move to the
`:targeted` map in backwards order, where the key is the _destination_ and the value is the _source_, using
`(update acc :targeted assoc elf' elf)`. If another elf is already planning to move to the target spot, then that
destination is now blocked such that neither of them move; we add the target location to the `:blocked` vector, put
both the previously targeted elf and the current elf into the `:staying` set, and then dissociate the previous elf from
the `:targeted` map. And of course, if the current elf is trying to move into a blocked position (making it the third
elf trying to go to a single space), it immediately moves to the `:staying` set.

Note that in the final line of the function, when we put the targeted elves into the set of staying elves, we remove
the _keys_ of the `:targeted` map, since the keys are the destination and not the source.

Now we're in the home stretch. As we don't know how many rounds we need to create, we'll create an infinite sequence of
elf boards using `elves-seq`.

```clojure
(defn elves-seq
  ([starting-elves] (elves-seq starting-elves direction-partitions))
  ([elves direction-seq]
   (lazy-seq (cons elves (elves-seq (move-elves elves (first direction-seq))
                                    (rest direction-seq))))))
```

This is a pretty normal infinite sequence function with two arities - a 1-arg version that just takes in the starting
elves, and a 2-arg version that takes in the elves and the current _sequence_ of direction partitions. Each time the
2-arg version is called, we return the current elves and lazily make a recursive call on the next state of elves with
the rest of the directional partitions. Note that, like any good sequence like this, the first value back should be the
original state of the elves, just as the first response from calling `iterate` should be the input data.

Now we can implement the `part1` function, giving us the number of empty spaces within the bounding box of the elves.

```clojure
(defn part1 [input]
  (let [elves' (-> (parse-elves input)
                   (elves-seq)
                   (nth 10))
        [[x-min y-min] [x-max y-max]] (p/bounding-box elves')]
    (- (* (- (inc x-max) x-min) (- (inc y-max) y-min))
       (count elves'))))
```

I do have an existing helper function within the `points` namespace called `bounding-box`, which takes in a collection
of points and returns two points representing the least `x` and `y` ordinates, and the greatest `x` and `y` ordinates,
in the form `[[x-min y-min] [x-max y-max]]`. If we calculate this on the 10th round of the `elves-seq`, then we just
have to multiply the differences to get the area of the box, and then subtract from that the number of elves. Note that
since `bounding-box` gives back the actual low and high values, when we calculate the lengths of `dx` and `dy`, we must
increment the max values (or decrement the min values), or else each dimension will be off by one.

Ok!  On to part 2.

---

## Part Two

Since we implemented `elves-seq` as an infinite sequence, there's nothing to do beside the `part2` function, where we
need to return the number of the first round where no elf moved.

```clojure
(defn part2 [input]
  (->> (parse-elves input)
       (elves-seq)
       (partition 2 1)
       (keep-indexed (fn [idx [e1 e2]] (when (= e1 e2) idx)))
       first
       inc))
```

After creating the sequence of elves, we again call `(partition 2 1)` to create pairs of every current and next
generation of elves. When the first matching pair exists, that means that nobody moved, so we just have to take the
first index where that occurs and increment it, since we want the index of the first generation that _didn't_ move,
rather than the last one that did.

And that's it! Nice and easy for day 23.