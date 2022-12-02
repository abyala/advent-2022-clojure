# Day 2: Rock Paper Scissors

* [Problem statement](https://adventofcode.com/2022/day/2)
* [Solution code](https://github.com/abyala/advent-2022-clojure/blob/master/src/advent_2022_clojure/day02.clj)
* [Simplest Solution code](https://github.com/abyala/advent-2022-clojure/blob/master/src/advent_2022_clojure/day02_simplest.clj)

---

## Prologue

I'm going to do this write-up slightly out-of-order on principle, in that I'm going to describe both parts of the
problem without writing any code.

Today's puzzle involves reading input strings that contain two characters, an `ABC` and an `XYZ` character. Based on
some parsing rules, each line is a round of Rock Paper Scissors, where we give 1, 2, or 3 points if the second player
picks rock, paper, or scissors, respectively; and then we give another 0, 3, or 6 points if the second player loses,
draws, or wins, respectively.

## Simplest solution

Well... the simplest way to solve this is to recognize that there are only 9 possible strings we can see on each line.
So after defining some simple constants for the points, we can make two maps - one for converting each string to its
point value in part 1 (where X, Y, Z maps to rock, paper, or scissors), and the other converting each strong to its
point value in part 2 (where X, Y, Z maps to lose, draw, win). Then we can use a transducer to apply the appropriate
map for each part, and add the results in the reducer.

```clojure
(def rock 1)
(def paper 2)
(def scissors 3)
(def loss 0)
(def draw 3)
(def win 6)

(def part1-points {"A X" (+ rock draw)
                   "A Y" (+ paper win)
                   "A Z" (+ scissors loss)
                   "B X" (+ rock loss)
                   "B Y" (+ paper draw)
                   "B Z" (+ scissors win)
                   "C X" (+ rock win)
                   "C Y" (+ paper loss)
                   "C Z" (+ scissors draw)})
(def part2-points {"A X" (+ scissors loss)
                   "A Y" (+ rock draw)
                   "A Z" (+ paper win)
                   "B X" (+ rock loss)
                   "B Y" (+ paper draw)
                   "B Z" (+ scissors win)
                   "C X" (+ paper loss)
                   "C Y" (+ scissors draw)
                   "C Z" (+ rock win)})

(defn solve [f input] (transduce (map f) + (str/split-lines input)))
(def part1 (partial solve part1-points))
(def part2 (partial solve part2-points))
```

Am I proud of this solution? Eh, it depends on where pride comes from.  But now that I got this obvious answer out of
the way, let's pretend this needed to be more elegant.

## More traditional approach

Ok, let's assume we were approaching this like any other problem.

### Part One

First we would focus on parsing, since we prefer to work with keywords as soon as we can stop thinking about how the
data is stored at rest. We make a map of each character to its keyword, and then map the first and third
character of each line to its keyword. While I would normally destructure the string using `[[c1 _ c2] line]`
in the `let` statement, we could also just pull the first and last character using `juxt` if we wanted to
avoid making extra `let` bindings. I probably would go with the bindings though for clarity.

```clojure
(def shape-map {\A :rock, \B :paper, \C :scissors
                \X :rock, \Y :paper, \Z :scissors})

; Typical implementation
(defn parse-line [line]
  (let [[c1 _ c2] line]
    (mapv shape-map [c1 c2])))

; Alternate
(defn parse-line [line]
  (mapv shape-map ((juxt first last) line)))
```

Then since we know both shapes selected, we need to determine the outcome of each round. We could accomplish
this with a simple conditional statement, looking for draws (both shapes are the same) or losses (spell out
the three losing conditions), with anything else being a win.

```clojure
(defn outcome [[p1 p2 :as round]]
  (cond
    (= p1 p2) :draw
    (#{[:rock :scissors] [:paper :rock] [:scissors :paper]} round) :loss
    :else :win))
```

Now that we know both the shape and the outcome, we can score the round. `round-points` maps the second
shape to its point value, and the result of the above `outcome` function to its points.

```clojure
(defn round-points [round]
  (+ ({:rock 1 :paper 2 :scissors 3} (second round))
     ({:loss 0 :draw 3 :win 6} (outcome round))))
```

Finally, we can make a simple 1-line `part1` function using a single transducer expression. The transformation function
is a composed mapping function, which first parses each line and then calculates its points. The results are reduced
using simple addition for each round.  This gets us our answer!

```clojure
(defn part1 [input]
  (transduce (map (comp round-points parse-line)) + (str/split-lines input)))
```

### Part Two

To do part 2, we need to do most of what we saw in part 1, but it's all just a little bit off.  First, we need to parse
the data such that the first character represents its shape and the second the outcome. So we'll make an `outcome-map`
to go along with the `shape-map`, and create a second parsing function.

```clojure
(def outcome-map {\X :loss \Y :draw \Z :win})

(defn parse-outcome-line [line]
  (let [[c1 _ c2] line]
    [(shape-map c1) (outcome-map c2)]))
```

Next we need to derive the second shape from the first shape and the outcome, we can write `outcome->shape` for this
transformation. Again, this is nothing more complicated than a simple map of the expected outcome and the first shape
to the second shape that matches.

```clojure
(defn outcome->shape [[p1 p2]]
  (case p2
    :loss ({:rock :scissors, :paper :rock, :scissors :paper} p1)
    :draw p1
    :win ({:rock :paper, :paper :scissors, :scissors :rock} p1)))
```
Finally, we can implement the `round-points2` and `part2` functions. The former calculates the required second shape
using `outcome->shape` and feeds it into the original `round-points` function, and the latter makes another transducer
leverage `parse-outcome-line` and `round-points2` instead of `parse-line` and `round-points`.

```clojure
(defn round-points2 [[p1 :as round]]
  (let [p2 (outcome->shape round)]
    (round-points [p1 p2])))

(defn part2 [input]
  (transduce (map (comp round-points2 parse-outcome-line)) + (str/split-lines input)))
```

--

## Refactor to common code

How do we bring the code all together for both parts? It doesn't make the code shorter, but it's not too bad. To start
off, I create two maps called `shape-rules` and `outcome-rules` to capture our so-called business logic. `shape-rules`
takes each shape and maps it to a map with the point value for using that shape, and a map called `:against` with the
outcome it would get if it was used against another shape. Likewise, `outcome-rules` shows the point value for that
outcome, as well as the shape a player would need if it wanted to have a certain outcome against a certain shape.

```clojure
(def shape-rules {:rock     {:points  1
                             :against {:rock :draw, :paper :loss, :scissors :win}}
                  :paper    {:points  2
                             :against {:rock :win, :paper :draw, :scissors :loss}}
                  :scissors {:points  3
                             :against {:rock :loss, :paper :win, :scissors :draw}}})
(def outcome-rules {:loss {:points  0
                           :against {:rock :scissors, :paper :rock, :scissors :paper}}
                    :draw {:points  3
                           :against {:rock :rock, :paper :paper, :scissors :scissors}}
                    :win  {:points  6
                           :against {:rock :paper, :paper :scissors, :scissors :rock}}})
```

Since we'll want the common code to use these two maps together, the `parse-line` function needs to provide all
possible data the code would need. Therefore, it will return a map with three keys - `:shape1` since we always know
that, `:shape2` for the shape parsing in part 1 and `:outcome2` for the outcome parsing in part 2.

```clojure
(defn parse-line [line]
  (let [[c1 _ c2] line]
    {:shape1   (shape-map c1)
     :shape2   (shape-map c2)
     :outcome2 (outcome-map c2)}))
```

Let's skip ahead and assume we're ready to score a round. This time, instead of receiving the actual round data from
the `round-points` and `round-points2` solutions we saw in the initial implementation, we'll instead take in the second
player's shape and outcome, not knowing how those values were derived. Then we use `get-in` with the two rules maps to
find the point values for the shape and outcome, and add them together.

```clojure
(defn score-round [shape outcome]
  (+ (get-in shape-rules [shape :points])
     (get-in outcome-rules [outcome :points])))
```

Now comes the fun part -- the `run-round` function. This shared function takes in two helper functions - one that
determines the second shape from a round, and another to determine the outcome. If we had those functions provided,
then this just needs to parse the line, call the `shape-fn` and `outcome-fn` arguments, and pass their results to the
`score-round` function.

```clojure
(defn run-round [shape-fn outcome-fn line]
  (let [round (parse-line line)]
    (score-round (shape-fn round) (outcome-fn round))))
```

So what are those functions? Well I made five helper functions to make the eventual `part1` and `part2` functions
super clear. The `parsed-shape1`, `parsed-shape2`, and `parsed-outcome2` functions simply access the values in the
output of the new and improved `parse-line` function. The `derived-shape2` and `derived-outcome2` functions use
`get-in` to call into the `shape-rules` and `outcome-rules` functions. For part 1, `derived-outcome2` uses the
`shape-rules` map (since we know the shape in part 1), to look up that shape's outcome against the first shape;
`derived-shape2` does similar work for part 2.

```clojure
(defn parsed-shape1 [round] (:shape1 round))
(defn parsed-shape2 [round] (:shape2 round))
(defn parsed-outcome2 [round] (:outcome2 round))
(defn derived-shape2 [round]
  (get-in outcome-rules [(parsed-outcome2 round) :against (parsed-shape1 round)]))
(defn derived-outcome2 [round]
  (get-in shape-rules [(parsed-shape2 round) :against (parsed-shape1 round)]))
```

How do we wrap this all up? With a `solve` function, of course! Once again we use a transducer, where the 
transformation function is a map that calls `run-round` with the `shape-fn` and `outcome-fn` it receives. And then
`part1` calls `solve` using the parsed shape and derived outcome, while `part2` calls `solve` using the derived shape
and the parsed outcome.

```clojure
(defn solve [shape-fn outcome-fn input]
  (transduce (map (partial run-round shape-fn outcome-fn)) + (str/split-lines input)))

(defn part1 [input] (solve parsed-shape2 derived-outcome2 input))
(defn part2 [input] (solve derived-shape2 parsed-outcome2 input))
```

So this rewrite is ever so slightly longer than the original, but its rules are nicely captured together, and the
program itself has a nice structure, so I'm happy with this solution.

But the `day02-simplest` solution is just plain lovelier though!