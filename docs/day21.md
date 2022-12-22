# Day 21: Monkey Math

* [Problem statement](https://adventofcode.com/2022/day/21)
* [Solution code](https://github.com/abyala/advent-2022-clojure/blob/master/src/advent_2022_clojure/day21.clj)

---

## Intro

I loved this puzzle! It was one of those where I solved part 1 at night, looked at part 2 and realized that brute force
wouldn't work, and then went to sleep with ideas about how to solve it swirling around. But it was such a gratifying
program to solve that I'm happy to have give myself time to sleep on it.

---

## Part One

Oh, those silly monkeys are at it again! We need to figure out which value the "root" monkey yells out, based on what
he hears from the other monkeys. 

### Preparations and parsing

To start, let's figure out the shape of our application state that we'll use in all of our major functions. I chose a
map with two keys -- for monkeys whose numeric yell value we haven't calculated yet, the `:monkeys` key will point to
a map of each monkey name to its dependent child monkeys and the operator to act on them; for monkeys whose numeric
yell value we _have_ calculated, the `:values` key will point to a map of the monkey name to its value.

    {:monkeys {:name {:dependencies [],
                      :op s}},
     :values {}}`

Before we implement the `parse-input` function, though, I want to introduce a few helper functions. We'll use these
throughout the code, including in the `parse-input` function itself, to make it all a bit easier to read.

```clojure
(def root "root")
(def human "humn")
(defn op->fn [s] ({"+" +, "-" -, "*" *, "/" quot, "=" =} s))

(defn dependencies-of [state name]
  (get-in state [:monkeys name :dependencies]))

(defn value-of [{:keys [values]} name]
  (or (values name) name))

(defn delete-monkey [state name]
  (update state :monkeys dissoc name))

(defn record-monkey-value [state name value]
  (-> state
      (delete-monkey name)
      (assoc-in [:values name] value)))

(defn record-monkey-deps [state name deps]
  (assoc-in state [:monkeys name :dependencies] deps))
```

`root` and `human` refer to the names of specific monkeys. We haven't seen the need to look at the `humn` monkey yet,
but we will in part 2. `op->fn` maps a single character string for the yelling operator into its actual Clojure
function; note the use of the `quot` function for division. `dependencies-of` just searches for the dependent monkeys
that one monkey will observe. `value-of` takes in the state and a monkey name, and either returns its yelling value
from the `:values` map, if known, or else the name again; this is a great place where the `or` function takes in
multiple values and returns the first non-`nil` one. `delete-monkey` just removes a monkey from the `:monkeys` map.
`record-monkey-value` will be called once we know what a monkey will yell, and includes calling `delete-monkey` in case
the monkey was previously "unknown." And finally, `record-monkey-deps` updates the dependent values that a monkey has.

Ok, now we're ready to parse the input.

```clojure
(defn parse-input [input]
  (reduce (fn [acc line] (let [name (subs line 0 4)
                               instruction (subs line 6)]
                           (if-some [n (parse-long instruction)]
                             (record-monkey-value acc name n)
                             (let [[m1 op m2] (str/split instruction #" ")]
                               (assoc-in acc [:monkeys name] {:dependencies [m1 m2] :op op})))))
          {:monkeys {} :values {}}
          (str/split-lines input)))
```

The basis of this is a single `reduce` function, starting with the empty map of monkeys and values, and operating over
the split lines of the input string. Then for each line, we strip it into two strings: the name (first four characters),
and the instruction (everything after the colon and space). If the instruction is a number, in which case `parse-long`
returns a non-`nil` value, we associate that monkey name with its value using `record-monkey-value`. If not, we split
the instruction into three parts -- the first child monkey, the operator, and the second child monkey -- and we
associate into the `:monkeys` map another map with the `:dependencies` (the two child monkeys) and the `:op` operator
string.

Alright, let's get to work.

### Solving the actual problem

Almost all the relevant work for part 1 sits in the `simplify` function, which takes in the parsed state and identifies
as many monkey yelling values as possible. If a monkey depends on two monkeys with known numeric values, we call
`record-monkey-value` to make that monkey's value also known by operating on the function in the proper order. This
should all culminate in the `root` monkey defining its value.

```clojure
(defn simplify
  ([state] (simplify state root))
  ([state name]
   (if (get-in state [:values name])
     state
     (let [deps (dependencies-of state name)
           state' (reduce simplify state deps)
           values' (map (partial value-of state') deps)
           op (get-in state' [:monkeys name :op])]
       (if (every? number? values')
         (record-monkey-value state' name (apply (op->fn op) values'))
         (record-monkey-deps state' name values'))))))
```

Since the input isn't too large this time around, I used a depth-first recursive search instead of the more
space-efficient depth-first search. The function provides two arities, the one just using the default starting node of
`root` for convenience. At each node, we first check to see if the monkey value is known; if so, we don't return that
value, but instead the current state since it cannot be simplified at this leaf node. Otherwise, we get the monkey's
dependencies and call `(reduce simplify state deps)` to invoke the `simplify` function on each monkey, feeding each of
their simplifications back into the running state, which we then bind to `state'`. We then extract out the values of
the now-simplified model for the two children, as well as the operation to perform. If both numbers are numeric, then
we have all the information we need to call `record-monkey-value` by applying `op->fn` for the operator onto the two
numeric values. If one or both are not numeric for some reason, then just update the dependencies and move on.

With this complete, we're ready to implement our very simple `part1` function.

```clojure
(defn part1 [input]
  (-> (parse-input input)
      simplify
      (get-in [:values root])))
```

We parse the input, simplify it down until every monkey has a value, and then grab the value for the root monkey.

---

## Part Two

### The approach

This part requires a slightly different approach, since we no longer what the `humn `monkey yelled, and in a cruel
twist of fate, we learn that we're the `humn` monkeys! Take that, creationists! With the `root` monkey forced to use
an equality operator, we need to determine what the human monkey should yell.

When I originally approached this problem, I thought about it the complete wrong direction. Namely, I thought that if
the map of unresolved monkeys were a tree or an equation, then we should start from the innermost element of the
equation, since algebraically that would be the inside of a pair of parentheses. It turns out that no, this is the
complete opposite approach from what we need. Rather, after simplifying the numeric monkeys with the existing
`simplify` function, we need to keep on pulling data from the _outside_ of the equation, until all that's left is the
`humn` monkey.

Let's take an example, using the structure of the original input instead of my map.

    root: aaaa = bbbb
    aaaa: cccc + dddd
    cccc: 3
    dddd: 7
    bbbb: eeee - ffff
    eeee: 14
    ffff: humn / gggg
    gggg: 5

First, we do some simplifications, since `aaaa` can quickly become `10`, and then that can find its way into the 
`root` definition too. Likewise, we can fully resolve `eeee` into `bbbb`, and `gggg` into `ffff`.

    root: 10 = bbbb
    bbbb: 14 - ffff
    ffff: humn / 5

First off, we can now recognize that every node other than the `root` should have one number and one monkey name, since
there is only one unknown monkey, and any pair of known monkeys would already have been simplified. So now we keep
looking at the `root` equation, and we solve for whatever the unknown child monkey value is. To solve the equation
`10 = bbbb = 14 - ffff` for `ffff`, we do some manipulation to end up with `ffff = 14 - 10`, and we redefine the `root`.

    root: ffff = 4
    ffff: humn / 5

We do the same thing again, solving `ffff = 4 = humn / 5` into `humn = 4 * 5`, defining `humn` as `20`.

    root: humn = 20

At each step, we redefine the `root` equation by operating on the child value, and pulling up the grandchild's
monkey name up to the `root` equation. So let's do it!

### Merging nodes up

Let's focus on how to merge a child node into the root parent node, using `merged-numeric-value`. I chose to pass in
all the data, rather than the state, so it'd be easier to reason with.

````clojure
(defn merged-numeric-value [parent-number child-number child-number-first? child-op]
  (cond
    (= child-op "+")                           (- parent-number child-number)
    (= child-op "*")                           (quot parent-number child-number)
    (and (= child-op "-") child-number-first?) (- child-number parent-number)
    (= child-op "-")                           (+ parent-number child-number)
    child-number-first?                        (quot child-number parent-number)
    :else                                      (* parent-number child-number)))
````

The function takes in four parameters:
* `parent-number` is the numeric value on one side of the root node.
* `child-number` is the numeric value on one side of the referred child node. The other side will have the grandchild's
node name.
* `child-number-first?` is a boolean value that says whether the child number comes before or after the grandchild's
node name. This is important for the non-commutative functions of `-` and `/`.
* `child-op` the string of the operator to apply to the two children.

Addition and multiplication are both commutative, so we don't care which child argument comes first. We pull these
values up by using subtraction or multiplication between the parent number and the child number.

    root:       10 = aaaa            root:       30 = cccc
    aaaa:        3 + bbbb            cccc:     dddd * 5
    ---------------------            ---------------------
    root: (10 - 3) = bbbb            root: (30 / 5) = dddd

Subtraction and division are a little tricky, because whether the child number comes first or second determines how to
solve the equation. A little napkin math will show when subtraction gets resolved using addition or subtraction, and
when division will get resolved using either multiplication or division.

For subtraction:

    root:      10 = aaaa             root:       10 = cccc
    aaaa:      18 - bbbb             cccc:     dddd - 7 
    --------------------             ---------------------
    root: (18-10) = bbbb             root: (10 + 7) = dddd
          subtraction                       addition

For division:

    root:      6 = aaaa              root:     6 = cccc
    aaaa:     18 / bbbb              cccc:  dddd / 4 
    -------------------              --------------------
    root: (18/6) = bbbb              root: (6*4) = dddd
          division                         multiplication

With that math out of the way, let's get back to our program.

### Finishing the puzzle

We now know how to merge a child node into the root once all the data is prepared, so now we need to drive the
logic to get to that `merged-numeric-value` function. For that, we'll use the `adjust-for-equality` function.

```clojure
(defn adjust-for-equality [state]
  (let [[d1 d2] (dependencies-of state root)
        left-numeric? (number? d1)
        child-node-name (if left-numeric? d2 d1)
        numeric-value (if left-numeric? d1 d2)]
    (if-some [{child-deps :dependencies, child-op :op} (get-in state [:monkeys child-node-name])]
      (let [[child-d1 child-d2] child-deps
            left-child-numeric? (number? child-d1)
            grandchild-node-name (if left-child-numeric? child-d2 child-d1)
            child-numeric-value (if left-child-numeric? child-d1 child-d2)
            merged-value (merged-numeric-value numeric-value child-numeric-value left-child-numeric? child-op)]
        (recur (-> state
                   (delete-monkey child-node-name)
                   (record-monkey-deps root [grandchild-node-name merged-value]))))

      ; If there is no child, then this is the terminal match on the equality, so bind it.
      (record-monkey-value state child-node-name numeric-value))))
```

I originally checked to ensure that the state included a `root` monkey with an `=` operator, but I stripped that out
for brevity. The goal now is to determine which side if the `root` node has a monkey name versus a number, and the
`left-numeric?` boolean will drive where to find the `child-node-name` and `numeric-value` bindings. Then we check to
see if the `child-node-name` refers to a known monkey (an intermediate one that eventually points to the `humn`) or an
unknown monkey (me, the human). If it's the human, then we can record that the `humn` monkey has the same value as the
other side of the equation, at which point we call `record-monkey-value` to have a complete state.

If not, then we need to prepare data to call `merged-numeric-value`, so we'll again create a `left-child-numeric?`
boolean, and use that to find the `grandchild-node-name` and `child-numeric-value` bindings. Then we call the above
`merged-value` function with our data, delete the intermediate monkey (sorry, buddy, but we don't care about you), 
and redefine the dependencies of the `root` with the merged value and the name of the grandchild node.

Finally, let's put it all together with the `part2` function, which I will not be merging into a shared `solve`
function.

````clojure
(defn part2 [input]
  (-> (parse-input input)
      (assoc-in [:monkeys root :op] "=")
      (update :values dissoc human)
      simplify
      adjust-for-equality
      (get-in [:values human])))
````

Now we parse the input, force the `root` monkey to use the `=` operator, and forget what we should the human monkey
would be yelling. Then we call `simplify` and `adjust-for-equality`, and then pull out the value of the human monkey to
get to our answer.
