(ns puget.data
  "Code to handle structured data, usually represented as EDN."
  (:require
    [clojure.data.codec.base64 :as b64])
  (:import
    (java.net URI)
    (java.text SimpleDateFormat)
    (java.util Date TimeZone UUID)))


;; TOTAL-ORDERING COMPARATOR

(defn- type-priority
  "Determines the 'priority' of the given value based on its type:
  - nil
  - Boolean
  - Number
  - Character
  - String
  - Keyword
  - Symbol
  - List
  - Vector
  - Set
  - Map
  - (all other types)"
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
  (or (some #(when-not (= 0 %) %)
            (map order xs ys))
      (- (count xs) (count ys))))


(defn total-order
  "Comparator function that provides a total-ordering of EDN values. Values of
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
        (compare-seqs total-order
          (sort-by first total-order (seq a))
          (sort-by first total-order (seq b)))

        (set? a)
        (let [size-diff (- (count a) (count b))]
          (if (not= size-diff 0)
            size-diff
            (compare-seqs total-order a b)))

        (coll? a)
        (compare-seqs total-order a b)

        :else
        (let [class-diff (compare (.getName (class a))
                                  (.getName (class b)))]
          (if (not= class-diff 0)
            class-diff
            (if (instance? java.lang.Comparable a)
              (compare a b)
              (compare (pr-str a) (pr-str b)))))))))



;; TAGGED VALUE PROTOCOL

(defprotocol TaggedValue
  (edn-tag [this] "Return the EDN tag symbol for this data type.")
  (edn-value [this] "Return the EDN value to follow the tag."))


(defn edn-str
  "Converts the given TaggedValue data to a tagged EDN string."
  ^String
  [v]
  (str \# (edn-tag v) \space (pr-str (edn-value v))))



;; EXTENSION FUNCTIONS

(defmacro defprint-method
  "Defines a print-method for the given class which writes out the EDN
  serialization from `edn-str`."
  [t]
  `(defmethod print-method ~t
     [v# ^java.io.Writer w#]
     (.write w# (edn-str v#))))


(defmacro extend-tagged-value
  "Extends the TaggedValue protocol with implementations which return the
  given symbol as the tag and use the given expression to calculate the value.
  The expression should resolve to a function which accepts one argument and
  returns the serialized value. This macro also defines a print-method which
  delegates to edn-str."
  [t tag expr]
  `(let [value-fn# ~expr]
     (extend-type ~t
       TaggedValue
       (edn-tag [this#] ~tag)
       (edn-value [this#]
         (value-fn# this#)))
     (defprint-method ~t)))


(defmacro extend-tagged-str
  [c tag]
  `(extend-tagged-value ~c ~tag str))


(defmacro extend-tagged-map
  [c tag]
  `(extend-tagged-value ~c ~tag
     (comp (partial into {}) seq)))



;; BUILT-IN EDN TAGS

; #inst - Date-time instant as an ISO-8601 string.
(defn- format-utc
  "Produces an ISO-8601 formatted date-time string from the given Date."
  [^Date date]
  (let [date-format (doto (java.text.SimpleDateFormat.
                            "yyyy-MM-dd'T'HH:mm:ss.SSS-00:00")
                       (.setTimeZone (TimeZone/getTimeZone "GMT")))]
    (.format date-format date)))


(extend-tagged-value Date 'inst format-utc)


; #uuid - Universally-unique identifier string.
(extend-tagged-str UUID 'uuid)


; #bin - Binary data in the form of byte arrays.
(extend-tagged-value
  (class (byte-array 0)) 'bin
  #(->> % b64/encode (map char) (apply str)))


(defn read-bin
  "Reads a base64-encoded string into a byte array."
  ^bytes
  [^String bin]
  (b64/decode (.getBytes bin)))


; #uri - Universal Resource Identifier string.
(extend-tagged-str URI 'uri)


(defn read-uri
  "Constructs a URI from a string value."
  ^URI
  [^String uri]
  (URI. uri))


; #??? - default handling function
(defrecord GenericTaggedValue
  [tag value]

  TaggedValue
  (edn-tag [this] tag)
  (edn-value [this] value))


(defprint-method GenericTaggedValue)


(defn tagged-value
  "Creates a generic tagged value record to represent some EDN value. This is
  suitable for use as a default-data-reader function."
  [tag value]
  {:pre [(symbol? tag)]}
  (->GenericTaggedValue tag value))
