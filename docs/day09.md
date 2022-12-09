# Day 9: Rope Bridge

* [Problem statement](https://adventofcode.com/2022/day/9)
* [Solution code](https://github.com/abyala/advent-2022-clojure/blob/master/src/advent_2022_clojure/day09.clj)

---

## Intro

What a fun puzzle today! I'm really happy with how simple the solution is, and I thoroughly enjoyed coding it up!

---

## Part One

Today we're pulling a rope with a knot in the front and one in the end, and we're watching how the tail knot follows us
around. The movement is over a 2-dimensional grid, where both knots start off at the origin. I won't explain the
movement here, as that's defined within the problem statement.

Let's think about how to represent the data. When I first implemented Part 1, I did so using a simple map of 
`{:head [x y], :tail [x y]}`, which worked fine. But... let's just say for hypothetical reasons, we may end up with more
knots in the rope in a little while. I can't imagine why, but let's just imagine it. So instead of a map, we'll use a
simple vector of knot positions, in this case being `[[x0 y0], [x1 y1]]`. Since we'll use a few other definitions from
the `point` namespace, we might as well initialize the `initial-rope` leveraging `p/origin`.

```clojure
(def initial-rope [p/origin p/origin])
```

Now let's focus on parsing the input string, which is a sequence of lines with the direction name and the number of
steps to take.

```clojure
(defn parse-line [line]
  (let [[dir amt] (str/split line #" ")]
    (repeat (parse-long amt)
            ({"L" p/left "R" p/right "U" p/up "D" p/down} dir))))

(defn parse-input [input]
  (mapcat parse-line (str/split-lines input)))
```

While normally I would parse an input line like `R 3` into something like `[[1 0] 3]`, this time I took a different
approach in `parse-line` - I transformed the data into `([1 0] [1 0] [1 0])` instead using the `repeat` function. It
was a perfectly acceptable approach for the size of the data, and it let me avoid using loops or recursive functions
or anything like that.

Then `parse-input` simply split each line, and flat mapped it using `mapcat` with the `parse-line` function to get a
single sequence of directional instructions.

Let's think ahead to a future function called `move`, which takes a state (the vector of knot coordinates) and a
direction. We'll want to move the head (first knot) in the instructed direction, and then pull the rest of the rope.

```clojure
(defn move [state dir]
  (-> state (move-head dir) pull-rope))
```

Moving the head is very simple. We just add the current `[x y]` coordinates of the first knot to those of the direction
in which it's being pulled.

```clojure
(defn move-head [state dir]
  (update state 0 (partial mapv + dir)))
```

Pulling the rope is a little trickier, as `pull-rope` also depends on `move-ordinate` and one helper function in the
`point` namespace.

```clojure
; advent-2022-clojure.point namespace
(defn touching? [[x0 y0] [x1 y1]]
  (and (<= (abs (- x0 x1)) 1)
       (<= (abs (- y0 y1)) 1)))

;advent-2022-clojure.day09 namespace
(defn move-ordinate [head-ord tail-ord]
  (condp apply [head-ord tail-ord] = tail-ord
                                   < (dec tail-ord)
                                   > (inc tail-ord)))

(defn pull-rope [state]
  (let [[head tail] state]
    (if (p/touching? head tail)
      state
      (update state 1 (partial mapv move-ordinate head)))))
```
Let's start with the `touching?` function in the `point` namespace. To know if the head and tail are touching, we need
to see if their respective `x` and `y` ordinates are adjacent. Thus, the absolute values of their differences cannot
exceed `1`.

Next let's skip down to `pull-rope`. We extract the two knots in the rope as `head` and `tail`, and check to see if they
are touching. If so, the tail doesn't move, so we just return the `state` as it was given. If they are no longer
adjacent, then we need to move the tail, which again is the one at index `1` in the state. For this, we note that we
can move the `x` and `y` ordinates independently, so we'll use `(mapv ordinate head tail)` to handle them separately.

`move-ordinate` isn't all that bad, thanks to one of my least favorite core functions, `condp`. It takes a predicate, 
an expression, and any number of test expression pairs. For each test expression, it calls the predicate and the test
to the expression; for the first test expression to give a truthy answer, the second element in the test expression pair
is returned. So for the first expression, `condp` essentially says `(if (apply = [head-ord tail-ord]] tail-ord))`. If
the `=` function fails, then it tries `(if (apply < [head-ord tail-ord]) (dec tail-ord))` and so on. This function says
that for two non-touching points, the ordinate of the tail will move one step "toward" the head.

Finally, we're ready to put it all together with the `part1` function.

```clojure
(defn part1 [input]
  (->> (parse-input input)
       (reductions move initial-rope)
       (map last)
       set
       count))
```

After parsing the input into the sequence of steps, we call one of my favorite functions, `reductions`. This does the
same thing as `reduce`, except it returns the reduced value after processing every element in its source collection,
rather than just the final value. So for each movement instruction, `reductions` will return the two-element vector of
head and tail coordinates. From there, we call `(map last)` since we only care about the tail, giving us a sequence of
positions where the tail was for each step. Throw those into a set and get the count, and we've got our solution!

---

## Part Two

Holy smokes, would you believe that our rope now has multiple knots? How unexpected!

Let's start by replaing our `initial-rope` function with a `create-rope` function, which takes in the number of knots.
We can simply call `(repeat n p/origin)` to return a sequence of `[0 0]` coordinates, which we then coerce into a
`vector` for direct access later on.

```clojure
(defn create-rope [n] (vec (repeat n p/origin)))
```

The only function we really have to change is `pull-rope`, since we now need to move each knot in order, rather than 
just the single tail knot. We'll convert this into a multi-arity function. The 2-arity version will do what it did in
part 1 -- given a state and the ID of the tail knot, move the tail as necessary; we use
`(map state [(dec knot-id) knot-id])` to pull both the `head` and `tail` bindings out at once. The 1-arity version
`reduce`s over each of the knots after the first, calling `pull-rope` so they move in order.

```clojure
(defn pull-rope
  ([state]
   (reduce pull-rope state (range 1 (count state))))

  ([state knot-id]
   (let [[head tail] (map state [(dec knot-id) knot-id])]
     (if (p/touching? head tail)
       state
       (update state knot-id (partial mapv move-ordinate head))))))
```

Now that we see the algorithm works the same for both puzzle parts, we can immediately create our `solve` function,
redefine `part1`, and create `part2`.

```clojure
(defn solve [knots input]
  (->> (parse-input input)
       (reductions move (create-rope knots))
       (map last)
       set
       count))

(defn part1 [input] (solve 2 input))
(defn part2 [input] (solve 10 input))
```

The only difference between the original `part1` and this `solve` function is that we call `(create-rope knots)`
instead of using the initial rope.  Then `part1` calls `solve` with a 2-knot rope, while `part2` calls it with a
10-knot rope. Lovely!

---

## Refactoring

Through the Clojurian Slack, I saw
[a great solution by nbardiuk](https://github.com/nbardiuk/adventofcode/blob/master/2022/src/day09.clj) that I really
liked. From his solution, I've reimplemented and simplified the `pull-rope` function.

```clojure
(defn pull-rope [state]
  (reduce (fn [acc tail] (let [head (last acc)]
                           (conj acc (if (p/touching? head tail)
                                       tail
                                       (mapv move-ordinate head tail)))))
          [(first state)]
          (rest state)))
```

First off, the function is now single arity again. In my original part2 solution, I was reducing over the indexes for
each knot within the `state` vector, using `update` to change values by index when needed. Instead, we now are reducing
over each knot (other than the head), starting from a clean vector containing just the head. As we go through each of
the items, we `conj` a new knot value to the end of it. If the reducing `tail` is touching the previous knot, we `conj`
it in place; otherwise, we `conj` the knot after calling `move-ordinate`. This is a much cleaner function than the
original one, I think!