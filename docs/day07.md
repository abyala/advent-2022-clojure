# Day 7: No Space Left On Device

* [Problem statement](https://adventofcode.com/2022/day/7)
* [Solution code](https://github.com/abyala/advent-2022-clojure/blob/master/src/advent_2022_clojure/day07.clj)

---

## Intro

This was an interesting puzzle today, because there are so many ways to represent the data, and there are a few ways
to "walk" the data. I'm documenting my original solution, and may come back later with alternate implementations if I
decide to try other options.

---

## Part One

The puzzle input is text representing terminal output, where each line is either a command instruction to change
directories or list the files, or describes a subdirectory relative to the current directory, or a file with its size
in the current directory. So as always, we start with parsing the data.

### Data format

The target structure I chose to represent is a recursive map. My original plan was to represent the data as such:

```clojure
{:files {"file1.txt" 100, "file2.txt" 200}
 :dirs  {"dir1" {:files {}, :dirs {}},
         "dir2" {:files {"file3.txt" 300}
                 :dirs  {"dir3" {:files {}, :dirs {}}}}}}
```

Thus a directory was a map with keys `:files` and `:dirs`. Every file in the `:files` map would map the filename to its
size, while the `:dirs` map would be a map from the directory name to the original data structure of `:files` and
`:dirs`. It turns out that we don't need the filenames in either part, so I ended up simplifying it by giving every
directory a ++shallow++ `:size` attribute.  I may go back later and make it a complete size instead.

```clojure
{:size 300, :dirs {"dir1" {:size   0, :dirs {}}
                   "dir2" {:size 300, :dirs {"dir3" {:size 0, :dirs {}}}}}}
```

### Parsing

To parse the incoming data, we'll need to work our way through the input lines, but at all times we must know what the
current directory of the parser is. For this, I want to quickly talk about the `current-dir` binding we'll use. This is
a vector that represents the current location of the file system within the state map. When at the root, it's an empty
vector. At every subdirectory, this is a vector of the keyword `:dirs` and the directory name. Thus, if we were at the
`dir3` directory in my example above, the value of `current-dir` would be `[:dirs "dir2" :dirs "dir3"]`. So this means
that whenever we change into a subdirectory, we must append both `:dirs` and the directory name, and whenever we move
up the file system we need to remove the last 2 values from the vector. For this, we'll use the `move-up` and
`move-down` helper functions.

```clojure
(defn move-up [current-dir]
  (subvec current-dir 0 (max 0 (- (count current-dir) 2))))

(defn move-down [current-dir sub-dir]
  (conj current-dir :dirs sub-dir))
```

To parse the incoming data, let's start with the top-level function and work our way down.  We'll quickly define an
`empty-directory` constant, and then go straight to `parse-input`.

```clojure
(def root-path [])
(def empty-directory {:size 0, :dirs {}})

(defn parse-input [input]
  (loop [lines (str/split-lines input), state empty-directory, current-dir root-path]
    (if-some [line (first lines)]
      (let [[arg0 arg1 arg2] (str/split line #" ")]
        (if (= arg0 "$")
          (recur (rest lines) state (cond (= arg1 "ls") current-dir
                                          (= arg2 "/") root-path
                                          (= arg2 "..") (move-up current-dir)
                                          :else (move-down current-dir arg2)))
          (recur (rest lines)
                 (if (= arg0 "dir")
                   (create-directory state current-dir arg1)
                   (create-file state current-dir (parse-long arg0)))
                 current-dir)))
      state)))
```

`empty-directory` is our map of 0 size and no directories. I wrote `parse-input` a few times, but ultimately decided it
was easiest to read as a `loop-recur` function, since we need to not only preserve the state of the file system, but
also the current directory of the parser. Within the loop, if the `lines` sequence is empty, we're done so we just
return the `state`. Otherwise, we split the line into up to 3 parameters.

If the first argument is a `$` string, we'the code is running a command. We don't have to do anything with `ls` because
the outputs will show up in the following lines. Otherwise, we're doing a `cd` command. `(cd "/")` means we reset to
the root path, and otherwise we use `move-up` or `move-down` as described above.

If the first argument is not a `$` string, then we're looking at the output of either a file or a directory. This code
went through a few iterations, based on whether the first string is the word `dir` we're going to either delegate to
`create-directory` or `create-file` functions. They're both very simple now that we've set up a nice vector to use for
our current directory.

```clojure
(defn create-directory [state current-dir dir-name]
  (assoc-in state (conj current-dir :dirs dir-name) empty-directory))

(defn create-file [state current-dir size]
  (update-in state (conj current-dir :size) + size))
```

Ok, we have parsed data! On to the problem itself.

### Puzzle logic

The goal is to figure out the total size of each directory, including all subdirectories. So we need to create a
`walk-dir-sizes` function, which takes in the parse file system and returns a map of every directory to its size. 

```clojure
(defn walk-dir-sizes
  ([fs] (walk-dir-sizes fs root-path))
  ([fs loc]
   (let [child-dir-names (keys (get-in fs (conj loc :dirs)))
         local-file-size (get-in fs (conj loc :size))
         sub-dirs (reduce (fn [acc child] (->> (move-down loc child) (walk-dir-sizes fs) (merge acc)))
                          {}
                          child-dir-names)
         sub-dir-sizes (transduce (map (comp sub-dirs (partial move-down loc))) + child-dir-names)]
     (assoc sub-dirs loc (+ sub-dir-sizes local-file-size)))))
```

First off, we create a multi-arity function, where the normal business logic would pass in just the file system, but
this delegates to the recursive call with the current location, again walking from the root path. For each directory
being inspected, we need to calculate the sum of all child directories, and then add them to the current directory's
size. The `sub-dirs` binding is the result of calling `reduce` on all child names; for each child, we use `walk-down`
to find that node's full path, recursively call `walk-dir-sizes` to get a resulting map, and then merge it in to a
blank map. This results in a map of every subdirectory to its complete size. To calculate this sum of all subdirectory
sizes, we use `transduce` to bind to `sub-dir-sizes`; in this case, for every subdirectory name, we again `move-down`
and map that value from the `sub-dirs` map just created, adding all values together. Finally, by adding the sum of all
child directory sizes to the current directory size.

Once that's done, we're ready for the `part1` function.

```clojure
(defn part1 [input]
  (->> (parse-input input)
       walk-dir-sizes
       vals
       (filter (partial >= 100000))
       (reduce +)))
```

It's pretty straightforward now -- parse the data, walk it, and call `vals` to look at just the sizes without caring
about which directory has which size. Then we filter down to the ones with a size at most 100000, and add the values
together with the `reduce` function.

Whew! That was a bunch for part 1.  Let's move on.

---

## Part Two

We have already done all of the work in part 1, so this should be a snap.

```clojure
(defn part2 [input]
  (let [sizes (-> input parse-input walk-dir-sizes)
        total-current-used (sizes root-path)
        target (- total-current-used 40000000)]
    (->> sizes
         vals
         (filter (partial < target))
         (apply min))))
```

First we need to know the minimum file size that will free up the necessary space, and that's just 40000000 minus the
size of the root path, which we can access with `(sizes root-path)`. Then it's just a simple filter and search for the
minimum value.

## Refactoring

Both parts 1 and 2 start off with similar logic - parse the data, walk through to get the sizes, and eventually throw
away the directory names so we can work with just the sizes. So let's see what we can do with an `input->sizes`
function which does just that.

```clojure
(defn input->sizes [input]
  (-> input parse-input walk-dir-sizes vals))
```

`part1` now just has to call that function, filter by file size, and add the values together. That could be a filter
and then a reduction... but that sounds like we've got another transducer on our hands! Let's do it.

```clojure
; No transducer?  Boooo!
(defn part1 [input]
  (->> (input->sizes input)
       (filter (partial >= 100000))
       (reduce +)))

; With a transducer!  Yay!
(defn part1 [input]
  (transduce (filter (partial >= 100000)) + (input->sizes input)))
```

Now `part2` _could_ be rewritten to use a transducer, but it becomes ugly because we need to implement all three forms
of the reducing function. Sometimes a `reduce` is just what we need.

```clojure
(defn part2 [input]
  (let [sizes (input->sizes input)
        target (- (reduce max sizes) 40000000)]
    (reduce min (filter (partial < target) sizes))))
```

Note that we now do two `reduce` calls. Since `input->sizes` hides the directory names, we need to call
`(reduce max sizes)` to find it again. Meh, no big deal. Then we call `(reduce min ...)` on the filtered sizes to get
a nice clean answer.
