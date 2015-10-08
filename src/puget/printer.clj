(ns puget.printer
  "Functions for canonical colored printing of EDN values."
  (:require
    [arrangement.core :as order]
    [clojure.string :as str]
    [fipp.printer :as fipp]
    [fipp.visit :as fv]
    [puget.color :as color]
    (puget.color ansi html)))


;; ## Control Vars

(def ^:dynamic *options*
  "Printer control options.

  #### General Rendering

  `:width`

  Number of characters to try to wrap pretty-printed forms at.

  `:print-meta`

  If true, metadata will be printed before values. If nil, defaults to the
  value of `*print-meta*`.

  `:sort-keys`

  Print maps and sets with ordered keys. Defaults to true, which will sort all
  collections. If a number, counted collections will be sorted up to the set
  size. Otherwise, collections are not sorted before printing.

  `:map-delimiter`

  The text placed between key-value pairs in a map.

  `:map-coll-separator`

  The text placed between a map key and a collection value. The keyword :line
  will cause line breaks if the whole map does not fit on a single line.


  #### Type Handling

  `:print-fallback`

  Keyword argument specifying how to format unknown values. The keyword
  `:print` will fall back to using `pr-str` rather than the default
  pretty-printed representation.

  `:escape-types`

  A set of symbols naming classes which should *not* be pretty-printed. Instead,
  they will be rendered as unknown values. This can be useful for types which
  define their own `print-method`, are extremely large nested structures, or
  which Puget otherwise has trouble rendering.

  `:strict`

  If true, throw an exception if there is no canonical EDN representation for
  a given value. This generally applies to any non-primitive value which does
  not extend `ExtendedNotation` and is not a built-in collection.


  #### Color Options

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
   :escape-types nil
   :print-fallback nil
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



;; ## Utility Functions

(defn- system-id
  "Returns the system id for the object as a hex string."
  [obj]
  (Integer/toHexString (System/identityHashCode obj)))


(defn- illegal-when-strict!
  "Throws an exception if strict mode is enabled. The error indicates that the
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
  "Dispatches the method to use for value formatting. Any types in the
  `:escape-types` set use the default formatter; values which use extended
  notation are rendered as tagged literals; others are dispatched on their
  `type`."
  [value]
  (let [class-sym (some-> value class .getName symbol)]
    (cond
      (contains? (:escape-types *options*) class-sym)
        :default

      (satisfies? data/ExtendedNotation value)
        ::tagged-literal

      :else (type value))))


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
   (case (:print-fallback *options*)
     :print [:span (pr-str value)]
     [:span
      (color-doc :class-delimiter "#<")
      (color-doc :class-name tag)
      (color-doc :class-delimiter "@")
      (system-id value)
      " "
      repr
      (color-doc :class-delimiter ">")])))



;; ## Clojure Types


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


(defmethod format-doc :default
  [value]
  (unknown-document value))


;; ## Printer Definition

(defrecord PugetPrinter

  fv/IVisitor

  ; Primitive Types

  (visit-nil
    [this]
    (color-doc :nil "nil"))

  (visit-boolean
    [this value]
    (color-doc :boolean (str value)))

  (visit-number
    [this value]
    (color-doc :number (pr-str value)))

  (visit-character
    [this value]
    (color-doc :character (pr-str value)))

  (visit-string
    [this value]
    (color-doc :string (pr-str value)))

  (visit-keyword
    [this value]
    (color-doc :keyword (str value)))

  (visit-symbol
    [this value]
    (color-doc :symbol (str value)))


  ; Collection Types

  (visit-seq
    [this value]
    (let [elements (if (symbol? (first value))
                     (cons (color-doc :function-symbol (str (first value)))
                           (map (partial fv/visit this) (rest value)))
                     (map (partial fv/visit this) value))]
      [:group
       (color-doc :delimiter "(")
       [:align (interpose :line elements)]
       (color-doc :delimiter ")")]))

  (visit-vector
    [this value]
    [:group
     (color-doc :delimiter "[")
     [:align (interpose :line (map (partial fv/visit this) value))]
     (color-doc :delimiter "]")])

  (visit-set
    [this value]
    (let [entries (order-collection value (partial sort order/rank))]
      [:group
       (color-doc :delimiter "#{")
       [:align (interpose :line (map (partial fv/visit this) entries))]
       (color-doc :delimiter "}")]))

  (visit-map
    [this value]
    (let [ks (order-collection value (partial sort-by first order/rank))
          entries (map (fn [[k v]]
                         [:span
                          (fv/visit this k)
                          (if (coll? v)
                            (:map-coll-separator *options*)
                            " ")
                          (fv/visit this v)])
                       ks)]
      [:group
       (color-doc :delimiter "{")
       [:align (interpose [:span (:map-delimiter *options*) :line] entries)]
       (color-doc :delimiter "}")]))


  ; Clojure Types

  (visit-meta
    [this m value]
    ...)

  (visit-var
    [this value]
    (illegal-when-strict! value)
    [:span
     (color-doc :delimiter "#'")
     (color-doc :symbol (subs (str value) 2))])

  (visit-pattern
    [this value]
    (illegal-when-strict! value)
    [:span
     (color-doc :delimiter "#")
     (color-doc :string (str \" value \"))])


  ; Special Types

  (visit-tagged
    [this value]
    (let [{:keys [tag form]} value]
      [:span
       (color-doc :tag (str "#" (:tag value)))
       " "
       (fv/visit this (:form value))]))

  (visit-unknown
    [this value]
    (unknown-document value)))



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
