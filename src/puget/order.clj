(ns puget.order
  "This namespace provides a total-ordering comparator for Clojure values.")


(defn- type-priority
  "Determines a numeric priority for the given value based on its general type:

  - `nil`
  - `false`
  - `true`
  - numbers
  - characters
  - strings
  - keywords
  - symbols
  - lists
  - vectors
  - sets
  - maps
  - all other types"
  [x]
  (let [predicates [nil? false? true? number? char? string?
                    keyword? symbol? list? vector? set? map?]
        priority (->> predicates
                      (map vector (range))
                      (some (fn [[i p]] (when (p x) i))))]
    (or priority (count predicates))))


(defn- compare-seqs
  "Compare sequences using the given comparator. If any element of the
  sequences orders differently, it determines the ordering. Otherwise, if the
  prefix matches, the longer sequence sorts later."
  [order xs ys]
  (or (first (remove zero? (map order xs ys)))
      (- (count xs) (count ys))))


(defn rank
  "Comparator function that provides a total ordering of EDN values. Values of
  different types sort in order of their types, per `type-priority`. `false`
  is before `true`, numbers are ordered by magnitude regardless of type, and
  characters, strings, keywords, and symbols are ordered lexically.

  Sequential collections are sorted by comparing their elements one at a time.
  If the sequences have equal leading elements, the longer one is ordered later.
  Sets are compared by cardinality first, then elements in sorted order.
  Finally, maps are compared by their entries in sorted order of their keys.

  All other types are sorted by class name. If the class implements
  `Comparable`, the instances of it are compared using `compare`. Otherwise, the
  values are ordered by print representation."
  [a b]
  (if (= a b) 0
    (let [pri-a (type-priority a)
          pri-b (type-priority b)]
      (cond
        (< pri-a pri-b) -1
        (> pri-a pri-b)  1

        (some #(% a) #{number? char? string? keyword? symbol?})
        (compare a b)

        (map? a)
        (compare-seqs rank
          (sort-by first rank (seq a))
          (sort-by first rank (seq b)))

        (set? a)
        (let [size-diff (- (count a) (count b))]
          (if (zero? size-diff)
            (compare-seqs rank a b)
            size-diff))

        (coll? a)
        (compare-seqs rank a b)

        :else
        (let [class-diff (compare (.getName (class a))
                                  (.getName (class b)))]
          (if (zero? class-diff)
            (if (instance? java.lang.Comparable a)
              (compare a b)
              (compare (pr-str a) (pr-str b)))
            class-diff))))))
