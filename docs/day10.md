# Day 10: Cathode-Ray Tube

* [Problem statement](https://adventofcode.com/2022/day/10)
* [Solution code](https://github.com/abyala/advent-2022-clojure/blob/master/src/advent_2022_clojure/day10.clj)

---

## Intro

Today's puzzle wasn't a particularly complicated one, but I did find the instructions a bit confusing at times. That's
another way of saying that at 1:00 in the morning when I was working on this, I was plagued by off-by-one errors and
had to wait until morning to clear my head before trying again.

---

## Part One

We're given a list of instructions from our CPU for manipulating a single memory register. Does this mean we're going
to have follow-up puzzles that build this into a full-fledged computer? It's hard to say just yet, so we'll keep the
solution simple this time.

To start off, let's figure out how to parse our instructions. Each one will either be a `noop`, which does nothing for
one cycle or an `addx`, which takes two cycles to increment the counter by some amount. I decided to implement a
`to-instructions` function, which converts each line into a sequence of functions to apply to the register, one for
each cycle.

```clojure
(defn to-instructions [s]
  (if (= s "noop")
    [identity]
    [identity (partial + (-> s (str/split #" ") second parse-long))]))
```

Since a `noop` does nothing to the register, it returns a vector of just the `identity` function. `addx` returns a
vector of `identity` first, and then a partial function to add the numeric value of the second argument to the
register.

Next, let's create a function called `signal-strengths`, which takes in the input and returns a vector of the register
values (signal strengths) after every clock cycle.

```clojure
(defn signal-strengths [input]
  (->> (str/split-lines input)
       (mapcat to-instructions)
       (reductions #(%2 %1) 1)
       vec))
```

We'll split the input into each line, and call `to-instructions` for each. We'll need to use `mapcat` instead of `map`
to unpack the collection coming out of each call to `to-instructions`. Then, as we saw in yesterday's puzzle, we'll
use `reductions` instead of `reduce` to invoke each instruction call onto the accumulated register, keeping each result,
and starting with an initial value of `1`. Finally, we'll use `vec` to change the sequence into a vector for easeier
access.

We're ready for the `part1` function and, surprise surprise, we'll be using `transduce` again!

```clojure
(defn part1 [input]
  (let [signals (signal-strengths input)]
    (transduce (map (fn [offset] (* offset (signals (dec offset))))) + [20 60 100 140 180 220])))
```

Our input collection is the list of cycle offsets the puzzle wants us to look at, namely 20, 60, 100, 140 180, and 220.
Then for each one, we'll map it to the product of itself and the signal at that offset, remembering to decrement its
value to handle the vector being 0-indexed. Finally, we add together those products to get our answer.

---

## Part Two

For part two, we need to send these offsets to a CRT for printing, where the printer goes in order across its
40-character screen and prints a value if the register is within one value of the printer's horizontal position. I
misunderstood these instructions a few times; the trick is if the printer is looking at position 2, it will print if
the register is 1, 2, or 3. The same is true if the printer is at position 42 - it still looks for 1, 2, or 3 since 42
is in the second position when we mod its value against the 40-width screen.

I implemented this in two ways, and while I like the second way much more, I thought it would be good to show both. I
find my first solution to be a bit more imperative, while the second to be cleaner and more functional.

But first, there was a convenience definition of `crt-width` to the value 40, and a helper function, `print-character`,
which I used in both solutions. This function takes in the position of the printer (the `crt-cycle`) and the `signal`,
and returns the value we want to print. I did decide to print out `x` for a print and a blank space for a miss, rather
than an `#` and a `.` as the instructions state. I just find it easier to read.

```clojure
(def crt-width 40)

(defn print-character [crt-cycle signal]
  (if (<= (abs (- crt-cycle signal)) 1) \x \space))
```

We simply need to see if the `crt-cycle` and the `signal` are at most one value apart for it to be considered a hit.
Note that in Clojure, you can either type `\ ` or `\space` to get the space character, but I find writing out `\space`
so much easier to read and debug. In fact, you can't really even see the space in `\ ` even in this markdown file.

### Solution 1: Imperative solution

```clojure
(defn part2 [input]
  (->> (signal-strengths input)
       (map-indexed #(print-character (mod %1 crt-width) %2))
       (partition crt-width)
       (map (partial apply str))
       (run! println)))
```

We start off by converting the input string into the `signal-strengths` vector, and then we proceed to map each value
in order to its printable character using `map-indexed`, remembering to call `(mod %1 crt-width)` to handle the printer
wrapping around after each 40-character line; this converts the example position 42 to its horizontal position of 2.

Now that we have a sequence of every character to print, we just have to break that sequence into partitions of 
40 characters, and then print them out to the screen using the `run!` function.  Note that it's arguably easier to
read without converting each row's character to sequence into a String first, but that's more an artifact of the data
being used.

As a string:

    xxx  xxx    xx  xx  xxxx  xx   xx  xxx  
    x  x x  x    x x  x    x x  x x  x x  x
    xxx  x  x    x x  x   x  x    x  x x  x
    x  x xxx     x xxxx  x   x xx xxxx xxx  
    x  x x    x  x x  x x    x  x x  x x    
    xxx  x     xx  x  x xxxx  xxx x  x x

As a sequence of characters:

    (x x x     x x x         x x     x x     x x x x     x x       x x     x x x    )
    (x     x   x     x         x   x     x         x   x     x   x     x   x     x  )
    (x x x     x     x         x   x     x       x     x         x     x   x     x  )
    (x     x   x x x           x   x x x x     x       x   x x   x x x x   x x x    )
    (x     x   x         x     x   x     x   x         x     x   x     x   x        )
    (x x x     x           x x     x     x   x x x x     x x x   x     x   x        )

### Solution 2: Functional solution

This time around, I decided I didn't much care for that `rem` function, so I went a different way.

```clojure
(defn part2 [input]
  (->> (map print-character (signal-strengths input) (cycle (range crt-width)))
       (partition crt-width)
       (map (partial apply str))
       (run! println)))
```

This time, I created two sequences that I sent to the `print-character` function using the `map` function. The first
sequence is just the vector of signal strengths that we've seen before. The second was an infinite sequence of values
from 0 to 39, using the `cycle` function. Doing this means that I don't need to play around with indexes and the `mod`
or `rem` function, but instead just combine the signal strengths directly to the CRT position I want. The rest of the
function is the same as in the first solution.

Both solutions get the job done, but there's something neat about zipping together these two sequences, without even
making an interim tuple sequence, that I found rather pleasing.
