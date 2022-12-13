# Day 12: Hill Climbing Algorithm

* [Problem statement](https://adventofcode.com/2022/day/12)
* [Solution code](https://github.com/abyala/advent-2022-clojure/blob/master/src/advent_2022_clojure/day12.clj)

---

## Part One

Climb every heightmap... ford every location with elevation no more than one above your previous elevation...  Today
we're going to take a hike through an alphabetic grid. We need to walk from position `S` to position `E`, going
to adjacent spaces so long as they're not too high to traverse. This was a problem in which I _thought_ I would have to
calculate the cumulative climbing cost of the path, but no, I didn't. So the code you'll see below assumes I knew this
all along!

### Parsing

Let's parse. I'd like to create a map with these three keys `{:grid {[x y] n}, :starts (p1 p2 p3...), :target [x y]}`.
The `:grid` key refers to a map of every x-y coordinate to its elevation value. The `:starts` key refers to the
possible starting positions, where we know there's only one in part 1. And the `:target` is the x-y coordinate of the
destination we're trying to reach.

We'll start with some helpers.

```clojure
(def start-char \S)
(def end-char \E)
(defn elevation [c] (or ({start-char 1, end-char 26} c)
                        (- (int c) 96)))
```

After creating the constants `start-char` and `end-char` (I hate repeating literals throughout my code!), I created a
small function called `elevation`, which maps every character in the grid to its elevation. The initial starting
character `S` and ending character `E` have the lowest and highest elevations respectively, while the rest  match their
index in the alphabet. Instead of a `case` or a `condp` function, I opted for a simple `or` function this time.
Namely, we either map the character to the start or end character and get back the values 1 or 26, or if the outcome is
`nil` (it's a different character), we just subtract 96 (one less than the value of `\a` so we can be 1-offset) instead.

I then made another helper function called `filter-for-keys`, which takes in a predicate and a map and returns the
keys of all map entries where the predicate is truthy. This could probably become a reusable function if there's value
in the future.

```clojure
(defn filter-for-keys [value-pred coll]
  (keep (fn [[k v]] (when (value-pred v) k)) coll))
```

Finally, it's time to create `parse-input`, taking in the set of allowed starting characters (should just be a set of
`start-char`) and the input string.

```clojure
(defn parse-input [starting-characters input]
  (let [grid (p/parse-to-char-coords-map input)]
    {:grid   (reduce-kv #(assoc %1 %2 (elevation %3)) {} grid),
     :starts (filter-for-keys starting-characters grid)
     :target (first (filter-for-keys #{end-char} grid))}))
```

We reuse the old `parse-to-char-coods-map` function, this time _without_ applying an initial transformation function,
since we need the original characters intact for now. We can then transform that grid of `{[x y] c}` to the expected
`{[x y] n}` using `reduce-kv`, on which we'll call `(assoc accumulating-map coords (elevation c))` using the
argument shorthand of `%1`, `%2`, and `%3`. To get the collection of `:starts`, which again should have one value,
we use `filter-for-keys` with the starting characters. And to find the target, since we _definitely_ know there's only
one value, we call `first` on `filter-for-keys` with the set of the `end-char`.

### Solving the puzzle

We know as we solve this puzzle that once we visit a space, there's never a reason to visit it again. So we want to
build up to a `possible-steps` function, which, given a grid, the set of spaces already visited, and the current
position, returns all adjacent spaces that are (1) on the map, (2) not already visited, and (3) are not at too high of
an elevation from the current space. For that, we'll implement `climbable?` and `possible-steps`.

```clojure
(defn climbable? [grid from to]
  (<= (grid to) (inc (grid from))))

(defn possible-steps [grid current]
  (filter (every-pred grid (partial climbable? grid current))
          (p/neighbors current)))
```

The `climbable?` function simply compares the elevation of the current and target locations, making sure that the
target isn't more than one more than the current.

`possible-steps` isn't too rough of a function either. It starts by calling `neighbors` from the `points` namespace,
which returns the coordinates in all four directions from the current point. From there, it filters for all of the
points that are both within the grid and are climbable.

I used the `every-pred` function here to show how we can easily compose multiple predicates into one, highlighting the
construction of a new predicate from multiple smaller ones. This whole function has the same effect as a more
imperative approach:

```clojure
; Another implementation without every-pred... this is simpler to look at, but less expressive I think.
(defn possible-steps [grid seen current]
  (->> (p/neighbors current)
       (filter grid)
       (filter (partial climbable? grid current))))
```

Now comes the heart of the problem - the `shortest-path` function, which takes in the grid, the target coordinates, and
the starting coordinates.

```clojure
(defn shortest-path [grid target start]
  (loop [options [{:current start, :moves 0}], seen #{}]
    (let [{:keys [current moves]} (first options)]
      (cond
        (= current target) moves
        (seen current) (recur (subvec options 1) seen)
        :else (let [neighbors (possible-steps grid current)
                    neighbor-options (map #(hash-map :current %, :moves (inc moves)) neighbors)
                    combined-options (apply conj options neighbor-options)]
                (when (seq combined-options)
                  (recur (subvec (apply conj options neighbor-options) 1) (conj seen current))))))))

```

I decided to use a `loop-recur` implementation due to the nature of the problem. Since we only care about the number of
steps taken, not the cost, we can apply a simple breadth-first search. As soon as we have one path to a space we haven't
seen before, we never need another way up there. So the lopo will use an `options` vector to show the possible locations
from which we can take the next step, and a `seen` set of all values we've already visited. We'll take the first value
from the vector each time we recurse, and add new possible destinations to the end. At each step, we apply a simple
conditional check - we're either at the target (return the number of moves taken), we've already seen the space (skip
it), or we are at a new spot and need to look around for all `possible-steps` to investigate.

To find our new options and recurse back in, we'll first find the available neighbors by calling `possible-steps`,
then map them to their `neighbor-options` by putting them into the proper structure of `{:current [x y], :moves n}`,
and then `conj` them onto the current vector of `options`. Why do this extra step? We'll find out in part two!

Now we can implement the `part1` function, which will look ever so slightly more complex than needed, due to what we
know is coming up in `part2`:

```clojure
(defn part1 [input]
  (let [{:keys [grid starts target]} (parse-input #{start-char} input)]
    (shortest-path grid target (first starts))))
```

We call `parse-input`, where the only allowed starting character is the `start-char` of `S`, and then after
destructuring the parsed input, we simply call `shortest-path` on the one starting coordinates received to get the
number of moves.

---

## Part Two

Well there's no surprise that now we need to contemplate multiple starting positions, namely either the `start-char` or
any other lowest elevation, meaning any coordinates starting with an `a` character. Now, instead of running 
`shortest-path` once, we'll need to run it with every possible starting position, and look for the shortest distance.
We can do this easily with a refactored `solve` function from `part1`:

```clojure
(def lowest-char \a)

(defn solve [starting-chars input]
  (let [{:keys [grid starts target]} (parse-input starting-chars input)]
    (->> starts
         (keep (partial shortest-path grid target))
         (reduce min))))

(defn part1 [input] (solve #{start-char} input))
(defn part2 [input] (solve #{start-char lowest-char} input))
```

We'll still call `parse-input` just as we did before, but now we'll need to map each possible starting position to its
distance, and `(reduce min)` the results. Instead of using `map`, we'll use `keep`, which again is a `map` function that
throws away any `nil` values. Why are we anticipating `nil` values? Well imagine a map that partially looked like this:

    Scaz
    bcdE    

From the starting position `S`, we could move south to `b`, and then east to `c`, `d`, and `E`. However, the letter
`a` has nowhere to move, so there would be no next step out of `shortest-path`, and thus the result would be `nil`.