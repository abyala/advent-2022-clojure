# Day 24: Blizzard Basin

* [Problem statement](https://adventofcode.com/2022/day/24)
* [Solution code](https://github.com/abyala/advent-2022-clojure/blob/master/src/advent_2022_clojure/day24.clj)

---

## Part One

Another day, another series of infinite sequences! Let's go.

We're given a grid of a maze with an entrance at the top, an exit at the bottom, and a number of directional blizzards.
We need to measure how many steps it will take to move from the entrance to the exit. I verified in all the test inputs
as well as the puzzle input that the entrance is always in the top leftmost corner, while the exit is always in the
bottom rightmost corner, which I used to slightly simplify the parsing.

To start, we'll parse in the data and create a map with five keys: `:blizzards`, `:entrance`, `:exit`, `:max-x`, and
`:max-y`.

```clojure
(defn parse-valley [input]
  (let [points (p/parse-to-char-coords input)
        blizzards (vec (keep (fn [[coords v]] (when-some [dir ({\> [1 0] \< [-1 0] \^ [0 -1] \v [0 1]} v)]
                                                [coords dir])) points))
        spaces-by-row (group-by second (keep #(when (= \. (second %)) (first %)) points))
        max-row (apply max (keys spaces-by-row))
        exit (first (spaces-by-row max-row))]
    {:blizzards blizzards
     :entrance  [1 0]
     :exit      exit
     :max-x     (first exit)
     :max-y     (dec (second exit))}))
```

Once again, we use `parse-to-char-coords` from the `advent2022-point` namespace to work with a sequence of 
`[coords v]` tuples. For the `:blizzards` key, we look for the four directional strings (`>`, `<`, `^`, `v`) and map
them to their `[dx dy]` pairs, where once again moving up means _decreasing_ the y-value. `keep` throws away the values
that are not blizzards. The entrance is always at `[1 0]`, and to find the exit we group all the points representing
any spaces based on their `y` coordinate (`second`), and then find the one and only value on that row using `first`.
Then as a convenience, we implement `:max-x` and `:max-y` to help determine the boundaries of allowed places for both
the expedition and the blizzards, which is the `x` ordinate of the exit (since it's in the last valid column), and one
less than the `y` ordinate of the exit (since the exit isn't actually within the maze).

Next, let's figure out how to make the blizzards move, and for this we'll make three functions: `move-blizzard`, which
moves a single blizzard to its next location, `move-blizzards`, which moves all blizzards within the valley, and
`blizzard-set-seq` which returns an infinite sequence of the valleys with the blizzards moving from place to place.

```clojure
(defn move-blizzard [max-x max-y [p dir :as blizzard]]
  (let [wall-x (inc max-x)
        wall-y (inc max-y)]
    (assoc blizzard 0 (match/match (mapv + p dir)
                                   [0 y] [max-x y]
                                   [wall-x y] [1 y]
                                   [x 0] [x max-y]
                                   [x wall-y] [x 1]
                                   [x y] [x y]))))

(defn move-blizzards [{:keys [blizzards max-x max-y] :as valley}]
  (assoc valley :blizzards (map (partial move-blizzard max-x max-y) blizzards)))

(defn blizzard-set-seq [valley]
  (->> (iterate move-blizzards valley)
       (map (comp set (partial map first) :blizzards))))

```

`move-blizzard` needs to know about the current blizzard (its position and direction), as well as the max `x` and `y`
values in the valley. I mentioned in the day 13 puzzle that I had considered using the
[clojure.core/match](https://github.com/clojure/core.match/wiki) namespace, and I decided to actually give it a whirl
this time for the sake of some nifty pattern matching. (I realized during the write-up that I could have simplified this
by using the `mod` function, possibly offsetting my interpretted points by making the leftmost valid value a `0` instead
of a `1`, but I used the `match` library successfully so I'm keeping it.)  Anyway, we start with moving the blizzard in
its direction using `(mapv + p dir)`, and then we check to see what the resulting `[x y]` look like. There are five
patterns to check for: the blizzard can be too far to the left (`x=0`), to the right (`x=max-x + 1`), to the top
(`y=0`), or to the bottom (`y=max-y + 1`), or else ths point is fine. We handle the wrapped values by setting the `x`
and `y` values to either a `1` or the `max-x`/`max-y` values. Again, `mod` would have worked nicely here. Anyway,
what's neat is that the matches look for the first pattern that makes sense, where we use either literal values like
`0`, `wall-x`, or `wall-y`, or locally-scoped bindings like `x` and `y` to find the correct pattern. It wasn't super
clean, since it depended on pre-calculating `(inc max-x)` and `(inc max-y)` to keep literals in the pattern, but that
wasn't too big a sacrifice to maek.

Then `move-blizzards` takes in a valley and moves every blizzard to its new location. We simplify `map` each blizzard
to use the `move-blizzards` function, and associate the new collection back into the valley.

Finally, `blizzard-set-seq` calls `(iterate move-blizzards)` to feed the valley over and over into `move-blizzards`,
and produces a sequence of blizzard locations by mapping each valley to the set of distinct points occupied by at least
one blizzard. Note that while the valley itself retains the full sequence of blizzards, including any duplicates that
exists for a time in the same location, the `blizzard-set-seq` deliberately returns sets of blizzard locations, as
that will help with future functions.

So we've parsed the input, found the dimensions, and can track the changing landscape of the valley. Now it's time to
move the expedtion. For this, we'll implement the functions `expedition-move-options` and `expedition-options-seq`,
where the former says where the expedition can be in the next time period, given the location of the blizzards, and the
latter represents the sequence of every location the expedition can be in as a sequence over time.

```clojure
(defn expedition-move-options [{:keys [blizzards entrance exit max-x max-y]} expedition]
  (filter (fn [[x y :as p]]
            (or (= p expedition entrance)
                (= p exit)
                (and (not (blizzards p))
                     (<= 1 x max-x)
                     (<= 1 y max-y))))
          (conj (p/neighbors expedition) expedition)))

(defn expedition-options-seq
  ([valley]
   (expedition-options-seq valley (rest (blizzard-set-seq valley))))

  ([valley [blizzards & next-blizzards]]
   (expedition-options-seq (assoc valley :blizzards blizzards) next-blizzards #{(:entrance valley)}))

  ([valley [blizzards & next-blizzards] expedition]
   (lazy-seq (cons expedition
                   (expedition-options-seq (assoc valley :blizzards blizzards)
                                           next-blizzards
                                           (set (mapcat (partial expedition-move-options valley) expedition)))))))
```

Let's start with `expedition-move-options`. This takes in the valley and one possible location of the expedition, and
returns the new possible values for the expedition. To do this, we start by looking in all four directions from the
current location using `(p/neighbors expedition), plus also the current location itself, in case the expedition doesn't
move on this turn. Each of these five locations are acceptable if either (1) the expedition is currently in the
entrance and wishes to stay there, (2) the expedition has a path to the exit, or (3) the expedition can be on a space
that's on the board and not occupied by a blizzard.

Then `expedition-options-seq` has three separate arities, depending on what we need at each phase of the solution.
We'll start with the 1-arg arity, which takes in just the valley, and assumes that the expedition starts in the
entrance and is using the `blizzard-set-seq` from the start. Since the expedition won't be moving from the starting
position of the blizzards, we'll immediately call `(rest)` on the `blizzard-set-seq` to get things going. The
2-arity version calls through to the 3-arity version, this time placing the first blizzard into the valley (since now
the wind has blown and the expedition needs to figure out where to go next), and again assumes the expedition starts
in the entrance. Finally, the 3-arity version takes in the valley, the sequence of blizzard sets, and the location of
the expedition, and it returns the lazy seqquence of possible expedition locations. In its lazy execution, it calls
itself again by placing the next blizzard set into the valley, and expanding the possible expeditions by calling
`mapcat`ting all of the `expedition-move-options` from the possible locations of the expedition.

So what we're left with is a sequence of sets of all possible places the expedition can be in.  Finally we can do
`part1`.

```clojure
(defn part1 [input]
  (let [{:keys [exit] :as valley} (parse-valley input)]
    (->> (expedition-options-seq valley)
         (take-until #(% exit))
         count
         dec)))
```

For `part1`, we parse the valley and create the sequence of expedition options. The goal is to figure out at which
generation the expedition could be in the exit, so we use `(take-until #(% exit))` to stop looking for new options once
the sequence includes the exit. Then we count the number of option sets we considered, throwing away the first one
since it was the starting position of the expedition.

---

## Part Two

We now need the expedition to go to the exit, back to the entrance, and back to the exit again. Silly people.

To do this, we now want to wrap our other infinite sequences into a new one, namely, how many steps it will take the
expedition to continue bouncing back and forth from entrance to exit and back again. Each leg of the journey will be
another value in the sequence. We'll need two functions here: `steps-to-exit` takes in a valley and a sequence of
blizzard sets, and returns the number of steps it took; `expedition-swap-seq` returns the sequence of steps for each
alternating journey taken.

```clojure
(defn steps-to-exit [valley blizzard-seq]
  (->> (expedition-options-seq valley blizzard-seq)
       (take-until #(% (:exit valley)))
       count
       dec))

(defn expedition-swap-seq
  ([valley] (expedition-swap-seq valley (rest (blizzard-set-seq valley))))
  ([valley blizzard-set]
   (let [num-steps (steps-to-exit valley blizzard-set)
         [entrance' exit'] ((juxt :exit :entrance) valley)]
     (lazy-seq (cons num-steps (expedition-swap-seq (assoc valley :entrance entrance' :exit exit')
                                                    (drop num-steps blizzard-set)))))))
```

`steps-to-exit`, effectively looks the same as the original `part1` function, except without the parsing.

`expedition-swap-seq` initially gets called with the valley, but then just as we saw in the 1-argument arity of the
`expeditions-options-seq`, it drops the original blizzard set before calling its 2-argument arity version. Here,
`expedition-swap-seq` counts how many steps it takes to get to the end, and then pulls out the current `exit` and
`entrance` of the valley, mapping them to their opposite values of `entrance'` and `exit'`. Finally, the lazy sequence
emits the number of steps taken for this leg, and call itself but binding the new entrance and exit to the valley, and
dropping the number of steps taken from the inifinite sequence of blizzard sets.

Finally, we can write the unified `solve` function and its `part1` and `part2` invocations.

```clojure
(defn solve [num-journeys input]
  (->> (parse-valley input)
       (expedition-swap-seq)
       (take num-journeys)
       (reduce +)))

(defn part1 [input] (solve 1 input))
(defn part2 [input] (solve 3 input))
```

`solve` asks for the number of journeys it needs to take, which is 1 for part 1 and 3 for part 2. Then it parses the
valley, creates the `expedition-swap-seq`, takes the number of journey lengths out that it needs, and adds them
together!

This puzzle wasn't hard per-se, but I definitely had two struggles. First, there were more than a few off-by-one errors
as I worked on this, mostly between lining up which generation of blizzard sets I was working on with which generation
of expeditions. Second, naming the functions was a bear this time around. There was `move-blizzard` and
`move-blizzards`, not to mention the distinction between blizzards, blizzard sets, and sequences of blizzard sets. And
this solution worked with three distinct infinite sequences (blizzard sets, expedition options, and expedition swaps)
that keeping these names in order was challenging. But that said, I really like how simple the actual logic is.
