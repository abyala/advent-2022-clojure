(ns advent-2022-clojure.utils
  (:require [clojure.string :as str]))

(defn split-by-blank-lines
  "Given an input string, returns a sequence of sub-strings, separated by a completely
  blank string. This function preserves any newlines between blank lines, and it filters
  out Windows' \"\r\" characters."
  [input]
  (-> (str/replace input "\r" "")
      (str/split #"\n\n")))

(defn split-blank-line-groups
  "Given an input string that represents multiple lines that get grouped together with blank line separators,
  returns a sequence of the line groups. Each line within a line group can optionally have a transformation
  function applied to it before being returned."
  ([input] (split-blank-line-groups identity input))
  ([xf input] (->> (str/split-lines input)
                   (partition-by str/blank?)
                   (take-nth 2)
                   (map (partial map xf)))))