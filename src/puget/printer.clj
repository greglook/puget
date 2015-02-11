(ns puget.printer
  "Functions for canonical colored printing of EDN values."
  (:require
    [clojure.string :as str]
    [fipp.printer :as fipp]
    (puget
      [color :as color]
      [data :as data]
      [order :as order])
    (puget.color ansi html)))


;; ## Control Vars

(def ^:dynamic *options*
  "Printer control options.

  `:width`
  Number of characters to try to wrap pretty-printed forms at.

  `:sort-keys`
  Print maps and sets with ordered keys. Defaults to true, which will sort all
  collections. If a number, counted collections will be sorted up to the set
  size. Otherwise, collections are not sorted before printing.

  `:strict`
  If true, throw an exception if there is no canonical EDN representation for
  a given value. This generally applies to any non-primitive value which does
  not extend puget.data/TaggedValue and is not a built-in collection.

  `:map-delimiter`
  The text placed between key-value pairs in a map.

  `:map-coll-separator`
  The text placed between a map key and a collection value. The keyword :line
  will cause line breaks if the whole map does not fit on a single line.

  `:print-meta`
  If true, metadata will be printed before values. If nil, defaults to the
  value of *print-meta*.

  `:print-color`
  When true, ouptut colored text from print functions.

  `:color-markup`
  :ansi for ANSI color text (the default),
  :html-inline for inline-styled html,
  :html-classes to use the names of the keys in the :color-scheme map
  as class names for spans so styling can be specified via CSS.

  `:color-scheme`
  Map of syntax element keywords to ANSI color codes."
  {:width 80
   :sort-keys true
   :strict false
   :map-delimiter ","
   :map-coll-separator " "
   :print-meta nil
   :print-color false
   :color-markup :ansi
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



;; ## Utility Functions

(defn- system-id
  "Returns the system id for the object as a hex string."
  [obj]
  (Integer/toHexString (System/identityHashCode obj)))


(defn- illegal-when-strict!
  "Throws an exception if strict mode is enabled. The error indincates that the
  given value has no EDN representation."
  [value]
  (when (:strict *options*)
    (throw (IllegalArgumentException.
             (str "No canonical EDN representation for " (class value) ": " value)))))


(defn- order-collection
  "Takes a sequence of entries and checks the `:sort-keys` option to determine
  whether to sort them. Returns an appropriately ordered sequence."
  [value sort-fn]
  (let [mode (:sort-keys *options*)]
    (if (or (true? mode)
            (and (number? mode)
                 (counted? value)
                 (>= mode (count value))))
      (sort-fn value)
      (seq value))))



;; ## Coloring Functions

(defn- color-doc
  "Constructs a text doc, which may be colored if `:print-color` is true.
  Element should be a key from the color-scheme map."
  [element text]
  (color/document element text *options*))


(defn color-text
  "Produces text colored according to the active color scheme. This is mostly
  useful to clients which want to produce output which matches data printed by
  Puget, but which is not directly printed by the library. Note that this
  function still obeys the `:print-color` option."
  [element text]
  (color/text element text *options*))



;; ## Formatting Multimethod

(defn- formatter-dispatch
  "Dispatches the method to use for value formatting. Values which use extended
  notation are rendered as tagged literals; others are dispatched on their
  `type`."
  [value]
  (if (satisfies? data/ExtendedNotation value)
    ::tagged-literal
    (type value)))


(defmulti format-doc
  "Converts the given value into a 'canonical' structured document, suitable
  for printing with fipp. This method also supports ANSI color escapes for
  syntax highlighting if desired."
  #'formatter-dispatch)


(defn- canonical-document
  "Constructs a complete canonical print document for the given value."
  [value]
  (let [print-meta? (if (nil? (:print-meta *options*))
                      *print-meta*
                      (:print-meta *options*))]
    (if-let [metadata (and print-meta? (meta value))]
      [:align
       [:span (color-doc :delimiter "^") (format-doc metadata)]
       :line (format-doc value)]
      (format-doc value))))


(defn- unknown-document
  "Renders common syntax doc for an unknown representation of a value."
  ([value]
   (unknown-document value (str value)))
  ([value repr]
   (unknown-document value (.getName (class value)) repr))
  ([value tag repr]
   (illegal-when-strict! value)
   [:span
    (color-doc :class-delimiter "#<")
    (color-doc :class-name tag)
    (color-doc :class-delimiter "@")
    (system-id value)
    " "
    repr
    (color-doc :class-delimiter ">")]))



;; ## Primitive Types

(defmacro ^:private format-element
  "Defines a canonization of a primitive value type by mapping it to an element
  in the color scheme."
  [dispatch element]
  `(defmethod format-doc ~dispatch
     [value#]
     (color-doc ~element (pr-str value#))))


(format-element nil                  :nil)
(format-element java.lang.Boolean    :boolean)
(format-element java.lang.Number     :number)
(format-element java.lang.Character  :character)
(format-element java.lang.String     :string)
(format-element clojure.lang.Keyword :keyword)
(format-element clojure.lang.Symbol  :symbol)



;; ## Collection Types

(defn- format-entry
  "Formats a canonical print document for a key-value entry in a map."
  [[k v]]
  [:span
   (format-doc k)
   (cond
     (satisfies? data/ExtendedNotation v) " "
     (coll? v) (:map-coll-separator *options*)
     :else " ")
   (format-doc v)])


(defn- format-map
  "Formats a canonical print document for a map value."
  [value]
  (let [ks (order-collection value (partial sort-by first order/rank))
        entries (map format-entry ks)]
    [:group
     (color-doc :delimiter "{")
     [:align (interpose [:span (:map-delimiter *options*) :line] entries)]
     (color-doc :delimiter "}")]))



(defmethod format-doc clojure.lang.ISeq
  [value]
  (let [elements (if (symbol? (first value))
                   (cons (color-doc :function-symbol (str (first value)))
                         (map format-doc (rest value)))
                   (map format-doc value))]
    [:group
     (color-doc :delimiter "(")
     [:align (interpose :line elements)]
     (color-doc :delimiter ")")]))


(defmethod format-doc clojure.lang.IPersistentVector
  [value]
  [:group
   (color-doc :delimiter "[")
   [:align (interpose :line (map format-doc value))]
   (color-doc :delimiter "]")])


(defmethod format-doc clojure.lang.IPersistentSet
  [value]
  (let [entries (order-collection value (partial sort order/rank))]
    [:group
     (color-doc :delimiter "#{")
     [:align (interpose :line (map format-doc entries))]
     (color-doc :delimiter "}")]))


(defmethod format-doc clojure.lang.IPersistentMap
  [value]
  (format-map value))


(defmethod format-doc clojure.lang.IRecord
  [value]
  (illegal-when-strict! value)
  [:span
   (color-doc :delimiter "#")
   (.getName (class value))
   (format-map value)])


(prefer-method format-doc clojure.lang.IRecord clojure.lang.IPersistentMap)



;; ## Clojure Types

(defmethod format-doc java.util.regex.Pattern
  [value]
  (illegal-when-strict! value)
  [:span
   (color-doc :delimiter "#")
   (color-doc :string (str \" value \"))])


(defmethod format-doc clojure.lang.Var
  [value]
  (illegal-when-strict! value)
  [:span
   (color-doc :delimiter "#'")
   (color-doc :symbol (subs (str value) 2))])


(defmethod format-doc clojure.lang.IDeref
  [value]
  (unknown-document value (format-doc @value)))


(defmethod format-doc clojure.lang.Atom
  [value]
  (unknown-document value "Atom" (format-doc @value)))


(defmethod format-doc clojure.lang.IPending
  [value]
  (let [doc (if (realized? value)
              (format-doc @value)
              (color-doc :nil "pending"))]
    (unknown-document value doc)))


(defmethod format-doc clojure.lang.Delay
  [value]
  (let [doc (if (realized? value)
              (format-doc @value)
              (color-doc :nil "pending"))]
    (unknown-document value "Delay" doc)))


(defmethod format-doc java.util.concurrent.Future
  [value]
  (let [doc (if (future-done? value)
              (format-doc @value)
              (color-doc :nil "pending"))]
    (unknown-document value "Future" doc)))


(prefer-method format-doc clojure.lang.ISeq clojure.lang.IPending)
(prefer-method format-doc clojure.lang.IPending clojure.lang.IDeref)
(prefer-method format-doc java.util.concurrent.Future clojure.lang.IDeref)
(prefer-method format-doc java.util.concurrent.Future clojure.lang.IPending)



;; ## Special Types

(defmethod format-doc ::tagged-literal
  [value]
  (let [{:keys [tag form]} (data/->edn value)]
    [:span
     (color-doc :tag (str \# tag))
     (if (coll? form) :line " ")
     (format-doc form)]))


(defmethod format-doc :default
  [value]
  (unknown-document value))



;; ## Printing Functions

(defn pprint
  "Pretty-prints a value to *out*. Options may be passed to override the
  default *options* map."
  ([value]
   (pprint value nil))
  ([value opts]
   (with-options opts
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
