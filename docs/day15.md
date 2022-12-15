# Day 15: Beacon Exclusion Zone

* [Problem statement](https://adventofcode.com/2022/day/15)
* [Solution code](https://github.com/abyala/advent-2022-clojure/blob/master/src/advent_2022_clojure/day15.clj)

---

## Intro

Today's puzzle looked tougher than I think it actually was, although there were definitely several steps needed to get
to a viable answer. My solution to part 2 isn't the fastest, clocking in at about 15 seconds for the full data set, but
I consider that good enough for my purposes.

I say that before other folks' solutions, which might inspire me to do a rewrite. Maybe I just shouldn't read what's
out there...

---

## Part One

In the first part, we've given a list of sensors detecting the closest beacons to them, such that all other points
within the same radius of the beacon from the sensor cannot have another beacon. We then need to detect how many
points along an infinite x-axis on row `10`, or row `2000000`, cannot have a beacon.

Let's start with parsing, which isn't bad this time. Each line in the input should be converted into a map with keys
`:sensor` and `:beacon`, both having values of `[x y]` coordinates.

```clojure
(defn parse-line [s]
  (let [[x1 y1 x2 y2] (map parse-long (re-seq #"-?\d+" s))]
    {:sensor [x1 y1] :beacon [x2 y2]}))

(defn parse-input [input]
  (->> input str/split-lines (map parse-line)))
```

`parse-line` grabs the four possibly-negative numeric strings out of the input, maps each to a long, and constructs the
map with the relevant keys. `parse-input` splits the input into individual lines, mapping each one with `parse-line`.

The general approach I took was to map each sensor-beacon pair to an `[x1 x2]` range, inclusive on both sides, which
represent the values on the target row which cannot be a beacon. To do this, we consider the input line where the
sensor is at `[0 11]` and the beacon is at `[2 10]`. We want to first calculate the radius of the circle from the
sensor to the beacon, then remove the distance from the sensor to the target row from the radius, and then gather the
values of `x` no greater than the remaining distance from the `x` ordinate of the sensor. In this case, the radius,
calculated with the Manhattan distance, is 3 (2 for dx and 1 for dy). To move from `[0 11]` to the target row 10, we
need to move a distance of `1`, leaving us with `2` remaining for the blocked off range. Finally, from the sensor's `x`
value of `0`, we go plus and minus `2` spaces, leaving us with an _inclusive_ range of `[-2 2]`. This corresponds to
the points `[[-2 10] [2 10]]` but I didn't need the `y` values anymore.

Now that we know the goal, let's check out `blocked-ranges-for-row`:

```clojure
(defn blocked-ranges-for-row [target-row readings]
  (keep (fn [{:keys [sensor beacon]}]
          (let [[sensor-x sensor-y] sensor
                total-distance (p/manhattan-distance sensor beacon)
                x-distance (- total-distance (abs (- target-row sensor-y)))]
            (when-not (neg-int? x-distance)
              [(- sensor-x x-distance) (+ sensor-x x-distance)])))
        readings))
```

The `target-row` in this case is `10` and `readings` is the sequence of parsed maps. After destructuring the sensor
and the beacon, and then the `x` and `y` ordinates out of the sensor, we calculate the Manhattan distance to the
beacon. Then from that distance, we get the `x-distance` by subtracting the different in `y` values between the sensor
and the target range. If that value is negative, then the beacon is closer to the sensor than the target row, which
means that the sensor isn't blocking anything on the row, and therefore can be ignored. Otherwise, we add or subtract
the `x-distance` from the sensor's `x` ordinate to get our inclusive range.

Now that we have a sequence of ranges, we want to consolidate them for simplicity reasons. The `combine-blocked-ranges`
should take care of that for us.

```clojure
(defn combine-blocked-ranges [ranges]
  (reduce (fn [acc [low' high' :as r]]
            (let [[low high] (last acc)]
              (cond
                (nil? low) [r]
                (<= low' (inc high)) (update-in acc [(dec (count acc)) 1] max high')
                :else (conj acc r))))
          []
          (sort ranges)))
```

This function takes in the sequence of inclusive `x` ranges, sorts them, and sends them through a `reduce` function.
By sorting the ranges, they will be ordered by their low values first, and then high values. Within the reducing
function, we compare the low and high values of each range to the last (rightmost) range in the accumulator. If nothing
has been accumulated yet, we have a vector with a single range. If the start of the next range is no greater than the
end of the previous range, then we set the end of the existing last range to be the greater of the two end values.
This covers multiple sceanios - the new range is contained within the old; the new range starts at the old start and
ends after the previous end; the new range starts within the old range but extends past it; or the new range starts
immediately after the old range, meaning that are adjacent and can be extended. Otherwise, this is a new,
non-overlapping that appears at the end of the current range.

Having taken in all of the readings and knowing where there the sensors have mapped ranges, we need to go back once
more and remove all of the points where there is a beacon. This is because each sensor says there can only be one
beacon within its radius, but we don't yet know if the beacon is on the target row. Thankfully, this is fairly easy:

```clojure
(defn num-beacons-for-row [target-row readings]
  (->> readings (map :beacon) (filter #(= target-row (second %))) distinct count))
```

Given the sequence of readings (again, maps of sensors and beacons), we pull out the beacons, filter for the ones
whose `y` ordinates match the target row, and count up the number of distinct values.

Finally, we're ready for `part1`.

```clojure
(defn part1 [row input]
  (let [readings (parse-input input)]
    (- (->> (combine-blocked-ranges (blocked-ranges-for-row row readings))
            (map (fn [[low high]] (inc (- high low))))
            (reduce +))
       (num-beacons-for-row row readings))))

(defn part1 [row input]
  (let [readings (parse-input input)
        ranges (combine-blocked-ranges (blocked-ranges-for-row row readings))]
    (- (transduce (map (comp inc abs (partial apply -))) + ranges)
       (num-beacons-for-row row readings))))
```

This function takes in both the target row and the input, and subtracts the number of beacons on the target row from
the number of blocked values in the ranges. We'll `transduce` to find the total number of blocked ordinates by
passing in the sequence of consolidated blocked ranges, calculating their size (one more than their difference to
account for the ranges being inclusive), and reducing with `+`.

Not so bad! Let's check out part 2.

---

## Part Two

We can solve part 2 with a single function using the building blocks we already have.

```clojure
(defn part2 [max-xy input]
  (let [readings (parse-input input)]
    (first (for [y (range 0 (inc max-xy))
                 :let [ranges (combine-blocked-ranges (blocked-ranges-for-row y readings))]
                 :when (= 2 (count ranges))
                 :let [x (-> ranges first second inc)]
                 :when (<= 0 x max-xy)]
             (+ (* x 4000000) y)))))
```

This function takes in the max `x` and `y` values we allow for the distress beacon's signal, which is 20 for the test
data and 4000000 for the puzzle data. To solve this function, instead of making some ugly nested sequence of maps,
filters, and bindings, I'm using `for` for list comprehension. We'll process over all possible values of `y`, from zero
to the `max-xy` value, binding `ranges` to the consolidated blocked ranges. For there to be a single distress beacon,
there must be exactly two ranges (theoretically with a single point between them) into which the beacon must sit.
Then on this row, we need to find where the `x` ordinate lies in said space, which is one more than the high value of
the first range. And while the input data doesn't necessarily require it, I do another guard check that makes sure this
`x` ordinate is within the necessary range too. If both conditions are met, we do the multiplication and division
requested by the puzzle. And since we further assume there's only one possible value that the `for` function could find,
we just call `first` to get to the answer.

I do like the simplicity of the `for` function and its many expressions. In this puzzle, I mixed one sequence
expressions with two `:let` bindings and two `:when` bindings for short-circuiting. The result is what I find to be a
very clear list of processes without nesting.
