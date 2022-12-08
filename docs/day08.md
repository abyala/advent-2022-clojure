# Day 8: Treetop Tree House

* [Problem statement](https://adventofcode.com/2022/day/8)
* [Solution code](https://github.com/abyala/advent-2022-clojure/blob/master/src/advent_2022_clojure/day08.clj)

---

## Part One

Today's puzzle starts with us reading a numeric grid that corresponds to trees, and our having to count the number of
trees that are visible from at least one direction, where visibility means that there are no trees between it and the
edge with equal or greater height.

To start, we need to parse the input string, and while this could easily come in as a 2-dimensional array, I just
prefer working with maps. I did create a helper function `char->int` since Clojure doesn't have a built-in function for
this conversion; in this case, we just leverage Java's `Character` class's static `digit` method.

```clojure
; advent-2022-clojure.utils namespace
(defn char->int
  "Reads a numeric character and returns its integer value, assuming base 10."
  [^Character c]
  (Character/digit c 10))

; advent-2022-clojure.day08 namespace
(defn parse-input [input]
  (reduce (fn [acc [k v]] (assoc acc k (char->int v)))
          {}
          (p/parse-to-char-coords input)))
```

For the parser, we're going to use a `reduce` function. The input collection `parse-to-char-coords` comes from a helper
namespace I've created in the past called `advent-2022-clojure.point`, since Advent project often work with `x-y`
coordinates, and there's no reason to reinvent this. Given its return of a sequence of `([[x y] c])`, we just assoc
the points into the map, converting the characters to ints using `char->int`.

Perhaps the most important function in the solution is `trees-in-direction`, which returns a lazy sequence of all tree
heights starting from a location and moving in one direction.

```clojure
(defn trees-in-direction [points p dir]
  (->> (iterate #(mapv + dir %) p)
       (map points)
       (next)
       (take-while some?)))
```

The `iterate` function takes a starting value and returns a lazy infinite sequence of the starting value and the result
of applying its first argument function to it. In this case, we're using `(mapv + p dir)` to map the `x` and `y`
coordinates of the given point to the `x` and `y` coordinates of a direction, such as up being `[0 1]`, by adding the
values together. Then we map each coordinate to its value in the `points` map to see what resides at that location.
After dropping the first value, since it's always the same as the initial value sent to the `iterate` function, we just
take all of the non-nil values by calling `(take-while some?)`.

So now that we've parse the input into a map of coordinates to their tree heights, and we can start from any position
and see the trees going out in any direction, we need to implement `visible?` to find out how many of them are visible
from any direction.

```clojure
(defn visible?
  ([points pos] (some #(visible? points pos %) p/cardinal-directions))
  ([points pos dir]
   (let [p (points pos)]
     (every? #(< % p) (trees-in-direction points pos dir)))))
```

The function has two arities for convenience. The 3-arity version has to make sure that every tree in a direction is
strictly shorter than the starting tree, so `every?` does that. Then the 2-arity version calls the 3-arity version to
see if there is any direction that is visible. Note that we must use `some` instead of `any?` here because the latter
returns true for _every_ input. 

It's time to wrap this up with our `part1` function.

```clojure
(defn part1 [input]
  (let [points (parse-input input)]
    (count-if #(visible? points %) (keys points))))
```

This is quite a simple function now. We use `(keys points)` to extract all of the coordinates in the map, and use our
fancy new `count-if` function to count how many of those coordinates pass the `visible?` predicate.

---

## Part Two

The second part is very similar to the first, and leverages much of the code. We need to find the point with the
greatest viewing distance, meaning the product of the number of trees the viewer can see from each direction.

```clojure
(defn viewing-distance
  ([points pos] (transduce (map #(viewing-distance points pos %)) * p/cardinal-directions))
  ([points pos dir]
   (let [p (points pos)]
     (reduce (fn [acc t] (if (>= t p) (reduced (inc acc)) (inc acc)))
             0
             (trees-in-direction points pos dir)))))
```

Like `visible?`, the `viewing-distance` function also has two arities. There are several ways to implement the
short-circuit feature of the 3-arity function, but I figured that a `reduce` function with a `reduced` call is the most
intuitive option. For each tree in a direction, the viewer can definitely see it, so we always call `(inc acc)`. But
if the tree blocks the view, we stop there with `reduced`, and if not, we keep going.

Then, as we tend to do in Advent this year, we use the `transduce` function again! Here we feed in the four cardinal
directions, and transform them into their viewing distances from the starting point. Finally, we reduce the values down
with the `*` multiplication function.

Then we just need `part2` and we're done.

```clojure
(defn part2 [input]
  (let [points (parse-input input)]
    (transduce (map (partial viewing-distance points)) max 0 (keys points))))
```

Well would you look at that -- it's another transducer! For each coordinate, we map it to its viewing distance, and
reduce it down to the max value. We do have to provide an initial value, because otherwise Clojure would try to call
`(max)` without any input, and there is no 0-arity version of the function. This contrasts the initial value `0` for
`+`, or `1` for `*`; there is no sensible starting value for `max`.
