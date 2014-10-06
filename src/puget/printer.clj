(ns puget.printer
  "Functions for canonical colored printing of EDN values."
  (:require
    [clojure.string :as str]
    [fipp.printer :as fipp]
    (puget
      [ansi :as ansi]
      [data :as data]
      [order :as order])))


;;;;; CONTROL VARS ;;;;;

(def ^:dynamic *options*
  "Printer control options.

  :width
  Number of characters to try to wrap pretty-printed forms at.

  :sort-keys
  Print maps and sets with ordered keys. Defaults to true, which will sort all
  collections. If a number, counted collections will be sorted up to the set
  size. Otherwise, collections are not sorted before printing.

  :strict
  If true, throw an exception if there is no canonical EDN representation for
  a given value. This generally applies to any non-primitive value which does
  not extend puget.data/TaggedValue and is not a built-in collection.

  :map-delimiter
  The text placed between key-value pairs in a map.

  :map-coll-separator
  The text placed between a map key and a collection value. The keyword :line
  will cause line breaks if the whole map does not fit on a single line.

  :print-meta
  If true, metadata will be printed before values. If nil, defaults to the
  value of *print-meta*.

  :print-color
  When true, ouptut ANSI colored text from print functions.

  :color-scheme
  Map of syntax element keywords to ANSI color codes."
  {:width 80
   :sort-keys true
   :strict false
   :map-delimiter ","
   :map-coll-separator " "
   :print-meta nil
   :print-color false
   :color-scheme
   {; syntax elements
    :delimiter [:bold :red]
    :tag       [:red]

    ; primitive values
    :nil       [:bold :black]
    :boolean   [:green]
    :number    [:cyan]
    :string    [:bold :magenta]
    :character [:bold :magenta]
    :keyword   [:bold :yellow]
    :symbol    nil

    ; special types
    :function-symbol [:bold :blue]
    :class-delimiter [:blue]
    :class-name      [:bold :blue]}})


(defn merge-options
  "Merges maps of printer options, taking care to combine the color scheme
  correctly."
  [a b]
  (let [colors (merge (:color-scheme a) (:color-scheme b))]
    (-> a (merge b) (assoc :color-scheme colors))))


(defmacro with-options
  "Executes the given expressions with a set of options merged into the current
  option map."
  [opts & body]
  `(binding [*options* (merge-options *options* ~opts)]
     ~@body))


(defmacro with-color
  "Executes the given expressions with colored output enabled."
  [& body]
  `(with-options {:print-color true}
     ~@body))


(defn set-color-scheme!
  "Sets the color scheme for syntax elements. Pass either a map to merge into
  the current color scheme, or a single element/colors pair. Colors should be
  vector of ANSI style keywords."
  ([colors]
   (alter-var-root #'*options* update-in [:color-scheme] merge colors))
  ([element colors & more]
   (set-color-scheme! (apply hash-map element colors more))))



;;;;; UTILITY FUNCTIONS ;;;;;

(defn- system-id
  [obj]
  (Integer/toHexString (System/identityHashCode obj)))


(defn- illegal-when-strict
  "Checks whether strict mode is enabled and throws an exception if so."
  [value]
  (when (:strict *options*)
    (throw (IllegalArgumentException.
             (str "No canonical EDN representation for " (class value) ": " value)))))


(defn- sort-entries
  "Takes a sequence of entries and determines whether to sort them. Returns an
  appropriately sorted (or unsorted) sequence."
  [value sort-fn]
  (let [mode (:sort-keys *options*)]
    (if (or (true? mode)
            (and (number? mode)
                 (counted? value)
                 (>= mode (count value))))
      (sort-fn value)
      (seq value))))



;;;;; COLORING FUNCTIONS ;;;;;

(defn- color-doc
  "Constructs a text doc, which may be colored if :print-color is true. Element
  should be a key from the color-scheme map."
  [element text]
  (let [codes (-> *options* :color-scheme (get element) seq)]
    (if (and (:print-color *options*) codes)
      [:span [:pass (ansi/esc codes)] text [:pass (ansi/escape :none)]]
      text)))


(defn color-text
  "Produces text colored according to the active color scheme. This is mostly
  useful to clients which want to produce output which matches data printed by
  Puget, but which is not directly printed by the library. Note that this
  function still obeys the :print-color option."
  [element text]
  (let [codes (-> *options* :color-scheme (get element) seq)]
    (if (and (:print-color *options*) codes)
      (str (ansi/esc codes) text (ansi/escape :none))
      text)))



;;;;; CANONIZE MULTIMETHOD ;;;;;

(defn- canonize-dispatch
  [value]
  (if (satisfies? data/TaggedValue value)
    :tagged-value
    (class value)))


(defmulti canonize
  "Converts the given value into a 'canonical' structured document, suitable
  for printing with fipp. This method also supports ANSI color escapes for
  syntax highlighting if desired."
  #'canonize-dispatch)


(defn- canonical-document
  "Constructs a complete canonical print document for the given value."
  [value]
  (let [print-meta? (if (nil? (:print-meta *options*))
                      *print-meta*
                      (:print-meta *options*))]
    (if-let [metadata (and print-meta? (meta value))]
      [:align
       [:span (color-doc :delimiter "^") (canonize metadata)]
        :line (canonize value)]
      (canonize value))))



;;;;; PRIMITIVE TYPES ;;;;;

(defmacro ^:private canonize-element
  "Defines a canonization of a primitive value type by mapping it to an element
  in the color scheme."
  [dispatch element]
  `(defmethod canonize ~dispatch
     [value#]
     (color-doc ~element (pr-str value#))))


(canonize-element nil                  :nil)
(canonize-element java.lang.Boolean    :boolean)
(canonize-element java.lang.Number     :number)
(canonize-element java.lang.Character  :character)
(canonize-element java.lang.String     :string)
(canonize-element clojure.lang.Keyword :keyword)
(canonize-element clojure.lang.Symbol  :symbol)



;;;;; COLLECTION TYPES ;;;;;

(defmethod canonize clojure.lang.ISeq
  [value]
  (let [elements (if (symbol? (first value))
                   (cons (color-doc :function-symbol (str (first value)))
                         (map canonize (rest value)))
                   (map canonize value))]
    [:group
     (color-doc :delimiter "(")
     [:align (interpose :line elements)]
     (color-doc :delimiter ")")]))


(defmethod canonize clojure.lang.IPersistentVector
  [value]
  [:group
   (color-doc :delimiter "[")
   [:align (interpose :line (map canonize value))]
   (color-doc :delimiter "]")])


(defmethod canonize clojure.lang.IPersistentSet
  [value]
  (let [entries (sort-entries value (partial sort order/rank))]
    [:group
     (color-doc :delimiter "#{")
     [:align (interpose :line (map canonize entries))]
     (color-doc :delimiter "}")]))


(defn- canonize-map
  [value]
  (let [canonize-kv
        (fn [[k v]]
          [:span
           (canonize k)
           (cond
             (satisfies? data/TaggedValue v) " "
             (coll? v) (:map-coll-separator *options*)
             :else " ")
           (canonize v)])
        ks (sort-entries value (partial sort-by first order/rank))
        entries (map canonize-kv ks)]
    [:group
     (color-doc :delimiter "{")
     [:align (interpose [:span (:map-delimiter *options*) :line] entries)]
     (color-doc :delimiter "}")]))


(defmethod canonize clojure.lang.IPersistentMap
  [value]
  (canonize-map value))


(defmethod canonize clojure.lang.IRecord
  [value]
  (illegal-when-strict value)
  [:span
   (color-doc :delimiter "#")
   (.getName (class value))
   (canonize-map value)])


(prefer-method canonize clojure.lang.IRecord clojure.lang.IPersistentMap)



;;;;; CLOJURE TYPES ;;;;;

(defmethod canonize java.util.regex.Pattern
  [value]
  (illegal-when-strict value)
  [:span
   (color-doc :delimiter "#")
   (color-doc :string (str \" value \"))])


(defmethod canonize clojure.lang.Var
  [value]
  (illegal-when-strict value)
  [:span
   (color-doc :delimiter "#'")
   (color-doc :symbol (subs (str value) 2))])



;;;;; OTHER TYPES ;;;;;

(defn- unknown-doc
  "Renders common syntax doc for an unknown representation of a value."
  ([value]
   (unknown-doc value (str value)))
  ([value repr]
   (unknown-doc value (.getName (class value)) repr))
  ([value tag repr]
   (illegal-when-strict value)
   [:span
    (color-doc :class-delimiter "#<")
    (color-doc :class-name tag)
    (color-doc :class-delimiter "@")
    (system-id value)
    " "
    repr
    (color-doc :class-delimiter ">")]))


(defmethod canonize clojure.lang.IDeref
  [value]
  (unknown-doc value (canonize @value)))


(defmethod canonize clojure.lang.Atom
  [value]
  (unknown-doc value "Atom" (canonize @value)))


(defmethod canonize clojure.lang.IPending
  [value]
  (unknown-doc value
    (if (realized? value)
      (canonize @value)
      (color-doc :nil "pending"))))


(defmethod canonize clojure.lang.Delay
  [value]
  (unknown-doc value "Delay"
    (if (realized? value)
      (canonize @value)
      (color-doc :nil "pending"))))


(defmethod canonize java.util.concurrent.Future
  [value]
  (unknown-doc value "Future"
    (if (future-done? value)
      (canonize @value)
      (color-doc :nil "pending"))))


(prefer-method canonize clojure.lang.ISeq clojure.lang.IPending)
(prefer-method canonize clojure.lang.IPending clojure.lang.IDeref)
(prefer-method canonize java.util.concurrent.Future clojure.lang.IDeref)
(prefer-method canonize java.util.concurrent.Future clojure.lang.IPending)



;;;;; SPECIAL TYPES ;;;;;

(defmethod canonize :tagged-value
  [tagged-value]
  (let [tag   (data/edn-tag tagged-value)
        value (data/edn-value tagged-value)]
    [:span
     (color-doc :tag (str \# tag))
     (if (coll? value) :line " ")
     (canonize value)]))


(defmethod canonize :default
  [value]
  (unknown-doc value))



;;;;; PRINT FUNCTIONS ;;;;;

(defn pprint
  "Pretty-prints a value to *out*. Options may be passed to override the
  default *options* map."
  ([value]
   (pprint value nil))
  ([value opts]
   (binding [*options* (merge *options* opts)]
     (fipp/pprint-document
       (canonical-document value)
       {:width (:width *options*)}))))


(defn pprint-str
  "Pretty-print a value to a string."
  ([value]
   (pprint-str value nil))
  ([value opts]
   (-> value
       (pprint opts)
       with-out-str
       str/trim-newline)))


(defn cprint
  "Like pprint, but turns on colored output."
  ([value]
   (cprint value nil))
  ([value opts]
   (with-color (pprint value opts))))


(defn cprint-str
  "Pretty-prints a value to a colored string."
  ([value]
   (cprint-str value nil))
  ([value opts]
   (with-color (pprint-str value opts))))
