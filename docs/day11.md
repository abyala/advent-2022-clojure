# Day 11: Monkey in the Middle

* [Problem statement](https://adventofcode.com/2022/day/11)
* [Solution code](https://github.com/abyala/advent-2022-clojure/blob/master/src/advent_2022_clojure/day11.clj)

---

## Intro

It had such a good start, today's puzzle. Then it became one of _those_ puzzles. Now here's the good news - I was able
to solve the puzzle without reading anyone else's code. And I _might_ know why I got to a working solution; only
tomorrow, when I read other folks' solutions, will I know for sure. But in the meantime, I will confidently document
my thoughts as though I'm right, and I'll adjust later if need be! (EDIT: I discovered why my solution worked, and have
adjusted the explanation below accordingly.)

In this puzzle, we're given some rather long descriptions of monkeys and how worried we are when they mess around with
our stuff. As we go from part 1 to part 2, apparently our anxiety meds wear off and our worry goes off the chart.

---

## Part One

### Parsing the data

I actually enjoyed parsing this data. Let's start with understanding how I want to represent each monkey, and as is
traditional for Clojure, a monkey is just a map. The keys I'm going to use are:
* `:id`: Just for readability
* `:worry-raiser`: The function to apply to increase worry over the item being inspected
* `:test-divisor`: Since all monkey test are checking divisibility (of prime numbers... ahem...), just keep the divisor
* `:true-monkey`: The target monkey to throw to if the adjusted worry level is divisible by `test-divisor`
* `:false-monkey`: The target monkey to throw to if the adjusted worry level is +not+ divisible by `test-divisor`
* `:inspections`: The accumulated number of items the monkey has inspected, defaulting to zero.

The only real interesting attribute here is `worry-raiser`. When I first solved the problem, I kept the entire string,
such as `"old * 19"`, which I parsed each time, since I thought it would be tricky to handle running operations on
either constants or the current worry level at runtime. Turns out no, it's not bad. So let's look at the parsing code.


```clojure
(defn parse-single-number [s]
  (parse-long (first (re-seq #"\d+" s))))

(defn parse-monkey [s]
  (let [[monkey-line starting-line op-line test-line true-line false-line] (str/split-lines s)]
    {:id           (parse-single-number monkey-line)
     :items        (mapv parse-long (re-seq #"\d+" starting-line))
     :worry-raiser (let [[_ op-str op-arg] (first (re-seq #".*old (\W) (.*)" op-line))
                         op-fn ({"*" * "+" +} op-str)]
                     (if (= "old" op-arg) (fn [v] (op-fn v v))
                                          (fn [v] (op-fn v (parse-long op-arg)))))
     :test-divisor (parse-single-number test-line)
     :true-monkey  (parse-single-number true-line)
     :false-monkey (parse-single-number false-line)
     :inspections  0}))
```

First off, `parse-single-number` is a little convenience function I'm using, since many of the input lines only contain
a single numeric string I'll search for and parse out using a simple regex. Then `parse-monkey` takes in a multi-line
string of the data that defines a monkey, which I split and destructure for easier parsing. The keys `:id`,
`:test-divisor`, `:true-monkey`, and `:false-monkey` just search their lines for their numeric digits, and
`:inspections` is a constant. So only `:items` and `:worry-raiser` are slightly more difficult.

For `:items`, we don't know how many starting items the monkey will have. `re-seq` will return a sequence of all
numeric strings on that line, which we'll then map to `parse-long`. Note that we will use `mapv` instead of `map`
because the instructions specifically state that each item thrown to a target monkey goes to the end of the recipient's
list. I don't know why they made such a big deal out of this, since it doesn't affect the outcome in any way, but I'm
feeling obedient so I'm using vectors instead of lists.

Finally, the `:worry-raiser` property is going to be a function taht takes in an `item` and returns its new worry level. We start
by using `re-seq` again, extracting out the symbol for the operator (which will either be `+` or `*`) and the next word,
which could be either `old` or a numeric string. We'll map the operator string to its function by sending it a map of
`{"*" *, "+" +}`, which I think just looks cool, but it's mapping a single-character string to an arithmetic function.
Finally, we check to see if the argument is `"old"` or something else, and we return one of two single-argument
functions we'll later invoke with the current item.

And then for cleanliness, we'll implement a simple `parse-monkeys` function that splits the input by its blank line,
and calls `parse-monkey` with each grouped string.

```clojure
(defn parse-monkeys [input]
  (mapv parse-monkey (u/split-by-blank-lines input)))
```

### Playing with monkeys

We already know how to raise worry when a monkey plays with an item, so now we need to make two other helper functions:
`reduce-worry` to relieve our stress once the monkey gets bored, and `next-monkey` to determine to whom the monkey
will send the item.

```clojure
; advent-2022-clojure.utils namespace
(defn divisible? [num denom]
  (zero? (rem num denom)))

; advent-2022-clojure.day11 namespace
(defn reduce-worry [item]
  (quot item 3))

(defn next-monkey [test-divisor true-monkey false-monkey item]
  (if (divisible? item test-divisor) true-monkey false-monkey))
```

`reduce-worry` is straightforward - the `quot` function divides the first argument by the second, throwing away the
remainder. And `next-monkey` just checks whether the item level is divisible by the test divisor, before choosing the
target monkey. While not strictly necessary, I implemented `divisible?` in the `utils` namespace, so it would be more
declarative in the application code and would be available for future puzzles.

Perhaps the trickiest function is `process-monkey`, and it's really not bad at all. This function takes in all of the
monkeys and the ID of the one doing the inspections, and returns the state of all monkeys once it's done playing.

```clojure
(defn process-monkey [monkeys monkey-id]
  (let [{:keys [items worry-raiser test-divisor true-monkey false-monkey]} (monkeys monkey-id)]
    (reduce (fn [monkeys' item]
              (let [item' (-> item worry-raiser reduce-worry)
                    target (next-monkey test-divisor true-monkey false-monkey item')]
                (update-in monkeys' [target :items] conj item')))
            (-> monkeys
                (assoc-in [monkey-id :items] [])
                (update-in [monkey-id :inspections] + (count items)))
            items)))
```

We can see that it's just a single `reduce` function, after destructuring the data within the monkey. Let's work
through the `reduce` arguments in reverse order.

The third argument is the collection to send in to the `reduce` function, which in this case is the vector of items
the monkey is about to inspect.

The second argument is the initial state of the accumulator, wherein we want to make two changes to the current state
of the monkeys. First, the monkey is about to give away all of its items, so we use `(assoc-in [monkey-id :items] [])`
to set its value to a new empty vector. Then we need to increase the number of inspections the monkey is about to do,
so we can call `(update-in [monkey-id :inspections] + (count items))` to achieve that. Part of what makes Clojure so
flexible is how its functions are so flexible across multiple data types. In this case, when we access `monkey-id`,
it's operating on a vector, which means it is being used as the index of the element within the vector. Then the
`:items` or `:inspections` arguments are applied to the monkey map, so they are being used as the keys into the map.
Clojure is happy to mix and match these accessors within a single `assoc-in` or `update-in` call. Neat!

The first argument of the `reduce` call is the reduction function, which takes in the accumulator (current state of
the monkeys) and the item being inspected. First, we increase our worry about the item with the `worry-raiser` of the
monkey, and then we lower it back down again with `reduce-worry`. Then we calculate the next monkey to own the item,
by passing in the new state of `items'`. Finally, we add the new item to the accumulated `monkeys'` vector, with
`(update-in monkeys' [target :items] conj item')`.

We're just about done!

```clojure
(defn process-monkeys [monkeys]
  (reduce process-monkey monkeys (range (count monkeys))))

(defn part1 [input]
  (->> (parse-monkeys input)
       (iterate process-monkeys)
       (drop 20)
       first
       (map :inspections)
       (sort >)
       (take 2)
       (apply *)))
```

The `process-monkeys` function takes the current state of the monkeys and calls `process-monkey` on each of them in
order. Since `process-monkey` takes in the monkeys and the index of the monkey about to get to work, we reduce over the
indexes of the monkeys, namely `(range (count monkeys))`.

Finally, `part1` just chugs through the components we've already built. `parse-monkeys` will parse the data into our
vector of monkey maps, and `(iterate process-monkeys)` creates and infinite sequence of calling `process-monkeys`.
We drop the first 20 and call `first` to get the iteration that matters, and then we `map` each of the monkeys to their
accumulated `:inspections` value. Then it's a simple matter of finding the two 2 and multiplying them together to get
our answer.

---

## Part Two

### Quick Math
Ok... part two. If we just run the code as described, it'll never complete. I did notice that all of the divisibility
tests were with prime numbers, which reminded me of another puzzle a few years ago that leveraged the Chinese
Remainder Theorem. I honestly did't remember how it worked, but I did an experiment -- since we don't reduce our
worry levels by dividing by 3 anymore, I checked to see what would happen if I took the remainder of the item level
after dividing it by the product of **all** of the test divisors. It worked, but it wasn't because of the Chinese
Remainder Theorem.

Here's the actual idea. Imagine we had only two monkeys with divisors of 3 and 5, and the current worry is 18,
remembering we no longer officially reduce our worry anymore. 18 is divisible by 3, so the test should return true.
Now after the next two iterations, let's say we add 5 and multiply by 10, so we'll go from 18 to 23 to 230. Now our
three tests (initial test and after passing the item twice) should resolve to `(true false true)`.

If we know that the divisors are 3 and 5, then that means that there are only 15 possible values of interest we need
to think about before they start repeating. It's exactly like the
[FizzBuzz puzzle](https://en.wikipedia.org/wiki/Fizz_buzz), in which the test value keeps incrementing by 1. So if we
mod our ever-increasing value by 15, the product of the divisors, we've essentially recreated FizzBuzz.

Taken another way, let's assume our divisors are again 3 and 5, so we're working with 3, and starting with an item
level of 28, which is the `(mod 15)` equivalent of 13. Let's play around with either incrementing the values or
incrementing and then using `(mod 15)`:

* Testing `[28 29 30 31 32 33]` against 3: `[false false true false false true]`
* Testing `[13 14  0  1  2  3]` against 3: `[false false true false false true]`
* Testing `[28 29 30 31 32 33]` against 5: `[false false true false false false]`
* Testing `[13 14  0  1  2  3]` against 5: `[false false true false false false]`

It's easier to prove out multiplication. If the item is divisible by 3, then multiplying the item by any other value
will keep it divisible by 3. Likewise, if the item is not divisible by 3, then the product will only be divisible by 3
if the multiplying factor itself is.

So, assuming _that_ makes any sense at all, let's get back to the code.

### Back to the puzzle

The first thing to do is to write a `create-worry-fn` which, like the original `worry-raiser` code in the `parse-monkey`
function, returns one of two functions. If we naturally reduce our worry, then we'll return a function that takes in
the item and divides it by 3, as we saw in the no longer needed `reduce-worry` function. If not, then we calculate the
`total-product`, and return a function that takes the remainder of the item after dividing by that product.

```clojure
(defn create-worry-reducer [reduce-worry? monkeys]
  (if reduce-worry?
    (fn [item] (quot item 3))
    (let [total-product (reduce * (map :test-divisor monkeys))]
      (fn [item] (rem item total-product)))))
```

Now we can refactor `process-monkey` and `process-monkeys` ever so slightly.

```clojure
(defn process-monkey [worry-reducer monkeys monkey-id]
  (let [{:keys [items worry-raiser test-divisor true-monkey false-monkey]} (monkeys monkey-id)]
    (reduce (fn [monkeys' item]
              (let [item' (-> item worry-raiser worry-reducer)
                    target (next-monkey test-divisor true-monkey false-monkey item')]
                (update-in monkeys' [target :items] conj item')))
            (-> monkeys
                (assoc-in [monkey-id :items] [])
                (update-in [monkey-id :inspections] + (count items)))
            items)))

(defn process-monkeys [worry-reducer monkeys]
  (reduce (partial process-monkey worry-reducer) monkeys (range (count monkeys))))
```

Both now take in a `worry-reducer` first argument, the result of calling `create-worry-fn`. `process-monkey` now calls
the `worry-reducer` function instead of the old `reduce-worry` function. Note that since `worry-reducer` is a
single-argument function, we can use the thread-first macro again with `(-> item worry-raiser worry-reducer)`. The
`process-monkeys` function now creates a partial function of `process-monkey` over `worry-reducer`, so it can be
called cleanly from `reduce` with its accumulator (`monkeys`) and its next value (`monkey-id`).

Finally, since we're so close to the end, let's build our `solve` function to power `part1` and `part2`.

```clojure
(defn solve [reduce-worry? num-iterations input]
  (let [monkeys (parse-monkeys input)
        worry-reducer (create-worry-reducer reduce-worry? monkeys)]
    (->> (iterate (partial process-monkeys worry-reducer) monkeys)
         (drop num-iterations)
         first
         (map :inspections)
         (sort >)
         (take 2)
         (apply *))))

(defn part1 [input] (solve true 20 input))
(defn part2 [input] (solve false 10000 input))
```

It's quite similar to the old `part1`, except that now we pre-create the parsed `monkeys` and use them to create the
`worry-reducer`. The function needs to know which worry reducer to use, as well as how many iterations to apply. The
`part1` function uses the "normal" worry reducer and 20 iterations, while the `part2` function uses the 
total product worry reducer and 10000 iterations.

And there you have it! A working solution, only slighly annoying on the basis of having to know math secrets.
