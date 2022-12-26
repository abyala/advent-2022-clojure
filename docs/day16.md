# Day 16: Proboscidea Volcanium

* [Problem statement](https://adventofcode.com/2022/day/16)
* [Solution code](https://github.com/abyala/advent-2022-clojure/blob/master/src/advent_2022_clojure/day16.clj)

---

## Intro

This puzzle was the first of several in a row this season where we are given an unreasonably large dataset or search
space to look through, and our goal is to figure out the trick to making it complete before the heat death of the
universe. I generally do not like this type of puzzle, but I understand that some do, so I did my best. While my
solution to part1 is fast for all inputs, and part2 is fast for the test input, it's quite slow for the full puzzle
input (about 4 minutes of running). That said, it works, and I'm not particularly motivated to speed it up further,
so I'm just going to document my correct but somewhat inefficient solution.

---

## Part One

We are given a list of rooms with valves in them, how much pressure each valve will begin releasing each turn once it's
opened, and the rooms adjacent to it. Our goal is to determine the most gas we can release over 30 turns, where each
turn can be spent either walking between adjacent rooms or opening the valve of the currently-occupied room. Also, by
inspecting the data, we can see that many rooms have broken valves, which means there's no reason to ever open those
valves, nor travel to those rooms except to get to another room with a working valve.

The bulk of this puzzle comes from not parsing the data, per se, but rather preparing it for manipulation.

### Visualizing the tunnel map

The goal we want to reach it to create a tunnel map (map of tunnels) of the format below:

    {room-name {:rate n, :tunnels {reachable-room-name distance}}}

Thus, if our tunnel looked like a very simple `A -- B -- C`, where B's valve is broken but A's was set to 5 and C's to
8, then the _intermediate_ tunnel map would look like:

    {"A" {:rate 5, :tunnels {"B" 1, "C" 2}},
     "B" {:rate 0, :tunnels {"A" 1, "C" 1}},
     "C" {:rate 8, :tunnels {"A" 2, "B" 1}}}

But the _final_ tunnel map would look like:

    {"A" {:rate 5, :tunnels {"C" 2}},
     "C" {:rate 8, :tunnels {"A" 2}}}

We want to entirely remove all traces of room "B" because we neither start in it, nor do we ever have to walk to it as
a destination. We can summarize the move from "A" to "C" as being a journey of 2 steps, without having to remember
where we went in-between.

There's a lot to do, so let's go through it step by step.

### Preparing the tunnel map

To start off, let's be able to parse each line of text into a vector of form `[room-name {:rate n :tunnels [names]}]`.

```clojure
(defn parse-tunnel [s]
  (let [[_ name rate tunnels] (re-matches #"Valve (\w+) has flow rate=(\d+); .* valves? (.*)" s)
        neighbors (reduce #(assoc %1 %2 1) {} (str/split tunnels #", "))]
    [name {:rate (parse-long rate) :tunnels neighbors}]))
```

There's nothing too complex here. We use `re-matches` to do regex work on the input, including treating the tunnels as
a single string of potentially multiple valves. We then make a map of each neighboring tunnel to its distance of 1,
since they are adjacent. Finally, we return the combined vector.

The initial pass of the tunnel map just combines each parsed line into a unified map.

```clojure
(defn parse-input [input]
  (into {} (map parse-tunnel (str/split-lines input))))
```

`all-connections` took me a good long while to write, and there is a ton going on in it. The goal is to take the map of
`{room-name {:rate n :tunnels {:neighbor-name 1}}}` into a similar map, but where the `:tunnels` map includes every
reachable room and its distance.

```clojure
(defn all-connections [parsed-connections]
  (let [initial-connections (reduce (fn [acc k] (assoc-in acc [k :tunnels] {}))
                                    parsed-connections
                                    (keys parsed-connections))
        initial-lessons (into {} (mapcat (fn [[from {:keys [tunnels]}]]
                                           (map #(vector [from %] 1) (keys tunnels)))
                                         parsed-connections))]
    (loop [connections initial-connections, lessons initial-lessons]
      (if (seq lessons)
        (let [[[from to :as journey] cost] (first lessons)]
          (if (> (get-in connections [from :tunnels to] Integer/MAX_VALUE) cost)
            (let [new-lessons (reduce (fn [acc [nested-dest nested-cost]]
                                        (assoc acc [nested-dest to] (+ nested-cost cost)
                                                   [to nested-dest] (+ nested-cost cost)))
                                      {}
                                      (dissoc (get-in connections [from :tunnels]) to))]
              (recur (update-in connections [from :tunnels] assoc to cost)
                     (merge-with min (dissoc lessons journey) new-lessons)))
            (recur connections (dissoc lessons journey))))
        connections))))
```

The `initial-connections` binding is our way of simply clearing out each `:tunnels` map; we'll build them back up again
soon. `initial-lessons` uses the concept of a "lesson" to mean "something we've learned about and want to feed into the
tunnel map," so it needs to be every connection we know about. We represent them as a vector of `[from to distance]`,
again where the starting distances are always `1`. Then we go through a `loop-recur` over all the lessons waiting to
be interpreted. Each time we find a new lesson, and it provides either a new or cheaper way for `from` to get to `to`,
we feed in all the new possible connections we've learned about, connecting the `to` room to all of `from`'s rooms, and
recurse them back in. When feeding the `new-lessons` into the remaining `lessons`, we use `(merge-with min)` so that
we only keep the cheapest distance between any two rooms. Finally, when we're done processing all of the lessons, the
`connections` map looks how we want it.

However, now that we know the cost between every two rooms, we want to remove all traces of the broken valves. For
this, we'll implement two functions `restrict-paths-to-rooms` and `remove-broken-valve-rooms`. The former eliminates
all rooms other than the ones passed in, or the `"AA"` starting room. The latter calls `restrict-paths-to-rooms` with
the names of all rooms that do not have broken valves.

```clojure
(defn restrict-paths-to-rooms [tunnel-map allowed-rooms]
  (reduce (fn [m k] (if (or (allowed-rooms k) (= "AA" k))
                      (update m k update :tunnels select-keys allowed-rooms)
                      (dissoc m k)))
          tunnel-map
          (keys tunnel-map)))

(defn remove-broken-valve-rooms [tunnel-map]
  (->> tunnel-map
       (keep (fn [[name {:keys [rate]}]] (when (pos-int? rate) name)))
       set
       (restrict-paths-to-rooms tunnel-map)))
```

`restrict-paths-to-rooms` does two things at once, as it goes through all the room names within the tunnel map. First,
if the room itself is not allowed nor `"AA"`, then it dissociates it from the map; it doesn't hurt anything to stay in
there since we'll never walk into those rooms, but they're just clutter. Second, it calls
`(select-keys m allowed-rooms)` on every reachable room, eliminating any connection that we never want to take.

`remove-broken-valve-rooms` looks through the tunnel map, searching for the names of rooms with a positive `:rate`,
converts them into a set, and then calls `restrict-paths-to-rooms`. We keep these two functions because we'll use the
former in part 2.

At last, we can fully prepare the tunnel map from scratch, using `prepare-tunne-map`. It just calls the other functions
we just went over, all in order.

```clojure
(defn prepare-tunnel-map [input]
  (-> (parse-input input) all-connections remove-broken-valve-rooms))
```

### Running the maze

It's time to do the actual work of walking through the tunnels. We'll start off defining out user state, being the part
that does change while the `tunnel-map` itself does not. The initial state defines the current room, the set of all
valves we've opened (initially none), the current pressure release rate (zero), the total pressure released so far
(also zero), and a countdown of how many more turns are permitted.

```clojure
(defn initial-state [total-time]
  {:room "AA" :open #{} :rate 0 :released 0 :time-remaining total-time})
```

On each turn, there are only two viable choices to make - either we take one or more turns to walk into an available
room and open the valve, or else give up and just sit in place, allowing the current pressure rate to release as much
pressure as possible. Let's start by looking at the `follow-tunnels-move` option.

```clojure
(defn follow-tunnels-move [tunnel-map state]
  (let [{:keys [room open time-remaining]} state]
    (keep (fn [[room' dist]] (let [time-after-move-and-open (- time-remaining dist 1)]
                               (when (and (not (open room'))
                                          (>= time-after-move-and-open 0))
                                 (let [released-in-transit (* (inc dist) (:rate state))]
                                   (-> (assoc state :room room' :time-remaining time-after-move-and-open)
                                       (update :open conj room')
                                       (update :released + released-in-transit)
                                       (update :rate + (get-in tunnel-map [room' :rate])))))))
          (get-in tunnel-map [room :tunnels]))))
```

This function looks at all the reachable tunnels from the current room (as defined by the `state`). We only need to
look at reachable rooms that we can reach before running out of time and which haven't already been opened yet. For
each one we fine, we calculate how much pressure will be released while we walk and open the valve, and then we return
a modified state - we change the room to the target neighbor, set the `:time-remaining` to its decreased value, mark
the target room's valve as being opened, increase the pressure released by the amount accumulating while in transit,
and then increase the new `:rate` in the `state` by the rate of the opened valve. This function then returns zero or
more next states in a sequence.

If we can't find a good next move, we sit around and wait, using the `give-up-move` function.

```clojure
(defn give-up-move [state]
  (let [{:keys [rate time-remaining]} state]
    (-> (assoc state :time-remaining 0)
        (update :released + (* time-remaining rate)))))
```

This one simply pushes the `:time-remaining` down to zero, and increases the state's `:released` value accordingly.

Now we can create a `next-steps` function, which takes in the tunnel map and state, and returns the collection of next
states to consider. It only retuns the coolection of the `give-up-move` if the `follow-tunnels-move` didn't return any
options.

```clojure
(defn next-steps [tunnel-map state]
  (let [options (follow-tunnels-move tunnel-map state)]
    (if (seq options)
      options
      [(give-up-move state)])))
```

Almost done. Now we implement `max-pressure-released`, which takes in the number of steps we can take (30) and the 
tunnel map, and returns the most accumulated pressure we can release across all available options.

```clojure
(defn max-pressure-released [max-steps tunnel-map]
  (loop [options [(initial-state max-steps)] most-pressure 0]
    (if-let [option (first options)]
      (if (zero? (:time-remaining option))
        (recur (rest options) (max most-pressure (:released option)))
        (recur (into (next-steps tunnel-map option) (rest options)) most-pressure))
      most-pressure)))
```

Once again we use a `loop-recur`, since as we move through the tunnels we will come up with additional options to
consider. For every option, we see if it has run out of time, in which case we decide whether it released more pressure
than other options or not. If there's time still remaining, we call `next-steps` to pick which alternatives it offers,
and feed them back into the next `recur`.

Finally, we can show `part1`, in which we just prepare the tunnel map and call `max-pressure-released`.

```clojure
(defn part1 [input]
  (max-pressure-released 30 (prepare-tunnel-map input)))
```

So that was a ton of work; 84 lines is a very large program in Clojure, I've found. But it's efficient and fast, so
let's just keep going.

---

## Part Two

The code for part 2 isn't too bad... even though the solution isn't great! We now need to figure out how much pressure
we can relieve with the help of a friendly, apparently dextrous elephant. We also need 4 minutes to train the elephant,
so we only get 26 moves instead of 30 this time.

The approach here is to split the rooms apart into the ones that "we" will open, and which the elephant will. Since we
only ever travel to a room to open a valve, and we never open them twice, we never need to set the same destination as
the elephant.

The `split-across-pairs` function takes in a collection of values, and returns all possible ways to split them across
each other. The function does ensure that `"AA"` is available as an initial room for both collections, and also forces
the first room in the input collection to be the humans' room; it doesn't much matter, but since `[(:a :b :c) (:d :e)]`
is effectively the same as `[(:d :e) (:a :b :c)]`, we can cut the search space in half by making sure that one
arbitrary room does not go to both explorers.

```clojure
(defn split-across-pairs [values]
  (reduce (fn [acc room] (concat (map #(update % 0 conj room) acc)
                                 (map #(update % 1 conj room) acc)))
          [[[(first values) "AA"] ["AA"]]]
          (rest values)))
```

The function is a single `reduce` call, where accumulator is a vector of two-element vectors. The initial value 
contains a single vector, where the first user has `"AA"` and the first value in the collection, and the second user
only has `"AA"`. Then for each room, we go through all the accumulated pairs, and either `conj` the new room to the
first or the second accumulator. Thus the size of the accumulation doubles with each added value.

Now that we can split a collection of room names into all viable pairs, we'll implement `tunnel-pairs` to create two
sets of `tunnel-map`s affiliated with the pairs.

```clojure
(defn tunnel-pairs [tunnel-map]
  (map (fn [pair] (map #(restrict-paths-to-rooms tunnel-map (set %)) pair))
       (split-across-pairs (keys tunnel-map))))
```

We start by calling `split-across-pairs` with all the keys in the `tunnel-map`, and then we `map` over each pair.
Within each pair, we `map` the room names in to `restrict-paths-to-rooms` with the "base" tunnel map, thus creating a
pair of tunnel maps.

We're in the home stretch.

```clojure
(defn max-pressure-for-pair [pair]
  (apply + (map (partial max-pressure-released 26) pair)))

(defn part2 [input]
  (let [tunnel-map (prepare-tunnel-map input)]
    (->> (tunnel-pairs tunnel-map)
         (map max-pressure-for-pair)
         (reduce max))))
```

`max-pressure-for-pair` determines the total amount of pressure that a pair of tunnel maps can relieve, by calling
`max-pressure-released` with 26 days, and adding the values from each tunnel map together. Finally, `part2` prepares
the base tunnel map, converts it into its tunnel pairs, maps each pair to its combined pressure, and then finds the
maximum value for all possible tunnel pairs.

Wow, this puzzle took a long long time to solve. I'm quite glad that's over!
