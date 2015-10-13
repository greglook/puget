(ns puget.printer
  "Enhanced printing functions for rendering Clojure values. The following
  options are available to control the printer:

  #### General Rendering

  `:width`

  Number of characters to try to wrap pretty-printed forms at.

  `:print-meta`

  If true, metadata will be printed before values. Defaults to the value of
  `*print-meta*` if unset.

  `:sort-mode`

  Print maps and sets with ordered keys. Defaults to true, which will sort all
  collections. If a number, counted collections will be sorted up to the set
  size. Otherwise, collections are not sorted before printing.

  `:map-delimiter`

  The text placed between key-value pairs in a map.

  `:map-coll-separator`

  The text placed between a map key and a collection value. The keyword :line
  will cause line breaks if the whole map does not fit on a single line.


  #### Type Handling

  `:print-handlers`

  A lookup function which will return a rendering function for a given class
  type. This will be tried before the built-in type logic. See the
  `puget.dispatch` namespace for some helpful constructors. The returned
  function should accept the current printer and the value to be rendered,
  returning a format document.

  `:print-fallback`

  Keyword argument specifying how to format unknown values. Puget supports a few
  different options:

  - `:pretty` renders values with the default colored representation.
  - `:print` defers to the standard print method by rendering unknown values
    using `pr-str`.
  - `:error` will throw an exception when types with no defined handler are
    encountered.
  - A function value will be called with the current printer options and the
    unknown value and is expected to return a formatting document representing
    it.


  #### Color Options

  `:print-color`

  When true, ouptut colored text from print functions.

  `:color-markup`

  :ansi for ANSI color text (the default),
  :html-inline for inline-styled html,
  :html-classes to use the names of the keys in the :color-scheme map
  as class names for spans so styling can be specified via CSS.

  `:color-scheme`

  Map of syntax element keywords to color codes.
  "
  (:require
    [arrangement.core :as order]
    [clojure.string :as str]
    [fipp.engine :as fe]
    [fipp.visit :as fv]
    [puget.dispatch :as dispatch]
    [puget.color :as color]
    (puget.color ansi html)))


;; ## Control Vars

(def ^:dynamic *options*
  "Default options to use when constructing new printers."
  {:width 80
   :sort-mode true
   :map-delimiter ","
   :map-coll-separator " "
   :print-fallback :pretty
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
    (assoc (merge a b) :color-scheme colors)))


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


(defn color-text
  "Produces text colored according to the active color scheme. This is mostly
  useful to clients which want to produce output which matches data printed by
  Puget, but which is not directly printed by the library. Note that this
  function still obeys the `:print-color` option."
  ([element text]
   (color-text *options* element text))
  ([options element text]
   (color/text options element text)))



;; ## Formatting Methods

(defn order-collection
  "Takes a sequence of entries and checks the `:sort-keys` option to determine
  whether to sort them. Returns an appropriately ordered sequence."
  [mode value sort-fn]
  (if (or (true? mode)
          (and (number? mode)
               (counted? value)
               (>= mode (count value))))
    (sort-fn value)
    (seq value)))


(defn format-unknown
  "Renders common syntax doc for an unknown representation of a value."
  ([printer value]
   (format-unknown printer value (str value)))
  ([printer value repr]
   (format-unknown printer value (.getName (class value)) repr))
  ([printer value tag repr]
   [:span
    (color/document printer :class-delimiter "#<")
    (color/document printer :class-name tag)
    (color/document printer :class-delimiter "@")
    (Integer/toHexString (System/identityHashCode value))
    " "
    repr
    (color/document printer :class-delimiter ">")]))


(defn format-doc*
  "Formats a document without considering metadata."
  [printer value]
  (let [lookup (:print-handlers printer)
        handler (and lookup (lookup (class value)))]
    (if handler
      (handler printer value)
      (fv/visit* printer value))))


(defn format-doc
  "Recursively renders a print document for the given value."
  [printer value]
  (if-let [metadata (meta value)]
    (fv/visit-meta printer metadata value)
    (format-doc* printer value)))



;; ## Canonical Printer Definition

(defrecord CanonicalPrinter
  [print-handlers]

  fv/IVisitor

  ; Primitive Types

  (visit-nil
    [this]
    "nil")

  (visit-boolean
    [this value]
    (str value))

  (visit-number
    [this value]
    (pr-str value))

  (visit-character
    [this value]
    (pr-str value))

  (visit-string
    [this value]
    (pr-str value))

  (visit-keyword
    [this value]
    (str value))

  (visit-symbol
    [this value]
    (str value))


  ; Collection Types

  (visit-seq
    [this value]
    (let [entries (map (partial format-doc this) value)]
      [:group "(" [:align (interpose " " entries)] ")"]))

  (visit-vector
    [this value]
    (let [entries (map (partial format-doc this) value)]
      [:group "[" [:align (interpose " " entries)] "]"]))

  (visit-set
    [this value]
    (let [entries (map (partial format-doc this)
                       (sort order/rank value))]
      [:group "#{" [:align (interpose " " entries)] "}"]))

  (visit-map
    [this value]
    (let [entries (map #(vector :span (format-doc this (key %))
                                " "   (format-doc this (val %)))
                       (sort-by first order/rank value))]
      [:group "{" [:align (interpose " " entries)] "}"]))


  ; Clojure Types

  (visit-meta
    [this metadata value]
    ; Metadata is not printed for canonical rendering.
    (format-doc* this value))

  (visit-var
    [this value]
    ; Defer to unknown, cover with handler.
    (fv/visit-unknown this value))

  (visit-pattern
    [this value]
    ; Defer to unknown, cover with handler.
    (fv/visit-unknown this value))


  ; Special Types

  (visit-tagged
    [this value]
    [:span (str "#" (:tag value)) " " (format-doc this (:form value))])

  (visit-unknown
    [this value]
    (throw (IllegalArgumentException.
             (str "No defined representation for " (class value) ": "
                  (pr-str value))))))



;; ## Pretty Printer Definition

(defrecord PrettyPrinter
  [sort-mode
   map-delimiter
   map-coll-separator
   print-handlers
   print-fallback
   print-meta
   print-color
   color-markup
   color-scheme]

  fv/IVisitor

  ; Primitive Types

  (visit-nil
    [this]
    (color/document this :nil "nil"))

  (visit-boolean
    [this value]
    (color/document this :boolean (str value)))

  (visit-number
    [this value]
    (color/document this :number (pr-str value)))

  (visit-character
    [this value]
    (color/document this :character (pr-str value)))

  (visit-string
    [this value]
    (color/document this :string (pr-str value)))

  (visit-keyword
    [this value]
    (color/document this :keyword (str value)))

  (visit-symbol
    [this value]
    (color/document this :symbol (str value)))


  ; Collection Types

  (visit-seq
    [this value]
    (let [elements (if (symbol? (first value))
                     (cons (color/document this :function-symbol (str (first value)))
                           (map (partial format-doc this) (rest value)))
                     (map (partial format-doc this) value))]
      [:group
       (color/document this :delimiter "(")
       [:align (interpose :line elements)]
       (color/document this :delimiter ")")]))

  (visit-vector
    [this value]
    [:group
     (color/document this :delimiter "[")
     [:align (interpose :line (map (partial format-doc this) value))]
     (color/document this :delimiter "]")])

  (visit-set
    [this value]
    (let [entries (order-collection sort-mode value (partial sort order/rank))]
      [:group
       (color/document this :delimiter "#{")
       [:align (interpose :line (map (partial format-doc this) entries))]
       (color/document this :delimiter "}")]))

  (visit-map
    [this value]
    (let [ks (order-collection sort-mode value (partial sort-by first order/rank))
          entries (map (fn [[k v]]
                         [:span
                          (format-doc this k)
                          (if (coll? v)
                            map-coll-separator
                            " ")
                          (format-doc this v)])
                       ks)]
      [:group
       (color/document this :delimiter "{")
       [:align (interpose [:span map-delimiter :line] entries)]
       (color/document this :delimiter "}")]))


  ; Clojure Types

  (visit-meta
    [this metadata value]
    (if print-meta
      [:align
       [:span (color/document this :delimiter "^") (format-doc this metadata)]
       :line (format-doc* this value)]
      (format-doc* this value)))

  (visit-var
    [this value]
    ; Defer to unknown, cover with handler.
    (fv/visit-unknown this value))

  (visit-pattern
    [this value]
    ; Defer to unknown, cover with handler.
    (fv/visit-unknown this value))


  ; Special Types

  (visit-tagged
    [this value]
    (let [{:keys [tag form]} value]
      [:span
       (color/document this :tag (str "#" (:tag value)))
       " "
       (format-doc this (:form value))]))

  (visit-unknown
    [this value]
    (case print-fallback
      :pretty
        (format-unknown this value)
      :print
        [:span (pr-str value)]
      :error
        (throw (IllegalArgumentException.
                 (str "No defined representation for " (class value) ": "
                      (pr-str value))))
      (if (ifn? print-fallback)
        (print-fallback this value)
        (throw (IllegalStateException.
                 (str "Unsupported value for print-fallback: "
                      (pr-str print-fallback))))))))



;; ## Type Handlers

(defn tagged-handler
  "Generates a handler function which renders a tagged-literal with the given
  tag and a value produced by calling the function."
  [tag value-fn]
  (fn handler
    [printer value]
    (format-doc printer (tagged-literal tag (value-fn value)))))


(def java-handlers
  "Map of common handlers for Java types."
  {java.util.regex.Pattern
   (fn pattern-handler
     [printer value]
     [:span
      (color/document printer :delimiter "#")
      (color/document printer :string (str \" value \"))])

   java.util.concurrent.Future
   (fn future-handler
     [printer value]
     (let [doc (if (future-done? value)
                 (format-doc printer @value)
                 (color/document printer :nil "pending"))]
    (format-unknown printer value "Future" doc)))

   java.util.Date
   (tagged-handler 'inst
     #(-> "yyyy-MM-dd'T'HH:mm:ss.SSS-00:00"
          java.text.SimpleDateFormat.
          (doto (.setTimeZone (java.util.TimeZone/getTimeZone "GMT")))
          (.format ^java.util.Date %)))

   java.util.UUID
   (tagged-handler 'uuid str)})


(def clojure-handlers
  "Map of common handlers for enhanced Clojure syntax."
  {clojure.lang.Var
   (fn var-handler
     [printer value]
     [:span
      (color/document printer :delimiter "#'")
      (color/document printer :symbol (subs (str value) 2))])

   clojure.lang.Atom
   (fn atom-handler
     [printer value]
     (format-unknown printer value "Atom" (format-doc printer @value)))

   clojure.lang.IPending
   (fn pending-handler
     [printer value]
     (let [doc (if (realized? value)
                 (format-doc printer @value)
                 (color/document printer :nil "pending"))]
    (format-unknown printer value doc)))

   clojure.lang.Delay
   (fn delay-handler
     [printer value]
     (let [doc (if (realized? value)
                 (format-doc printer @value)
                 (color/document printer :nil "pending"))]
       (format-unknown printer value "Delay" doc)))})


(def common-handlers
  (dispatch/chained-lookup
    (dispatch/inheritance-lookup java-handlers)
    (dispatch/inheritance-lookup clojure-handlers)))



;; ## Printing Functions

(defn canonical-printer
  "Constructs a new canonical printer with the given handler dispatch."
  ([]
   (canonical-printer nil))
  ([handlers]
   (map->CanonicalPrinter
     {:width 0
      :print-handlers handlers})))


(defn pretty-printer
  "Constructs a new printer from the given configuration."
  [opts]
  (->> [{:print-meta *print-meta*
         :print-handlers common-handlers}
        *options*
        opts]
       (reduce merge-options)
       (map->PrettyPrinter)))


(defn render-out
  "Prints a value using the given printer."
  [printer value]
  (binding [*print-meta* false]
    (fe/pprint-document
      (format-doc printer value)
      {:width (:width printer)})))


(defn render-str
  "Renders a value to a string using the given printer."
  [printer value]
  (str/trim-newline
    (with-out-str
      (render-out printer value))))


(defn pprint
  "Pretty-prints a value to *out*. Options may be passed to override the
  default *options* map."
  ([value]
   (pprint value nil))
  ([value opts]
   (render-out (pretty-printer opts) value)))


(defn pprint-str
  "Pretty-print a value to a string."
  ([value]
   (pprint-str value nil))
  ([value opts]
   (render-str (pretty-printer opts) value)))


(defn cprint
  "Like pprint, but turns on colored output."
  ([value]
   (cprint value nil))
  ([value opts]
   (pprint value (assoc opts :print-color true))))


(defn cprint-str
  "Pretty-prints a value to a colored string."
  ([value]
   (cprint-str value nil))
  ([value opts]
   (pprint-str value (assoc opts :print-color true))))
