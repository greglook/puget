(ns puget.printer
  "Functions for canonical colored printing of EDN values."
  (:require
    [clojure.string :as string]
    [fipp.printer :refer [defprinter]]
    (puget
      [ansi :as ansi]
      [data :as data])))


;; CONTROL VARS

(def ^:dynamic *colored-output*
  "Output ANSI colored output from print functions."
  false)


(def ^:dynamic *strict-mode*
  "If set, throw an exception if there is no defined canonical print method for
  a given value."
  false)



;; COLORING FUNCTIONS

(defn- color-text
  "Constructs a text doc, which may be colored if *colored-output* is true."
  [text & codes]
  (if (and *colored-output* (not (empty? codes)))
    [:span [:pass (ansi/esc codes)] text [:pass (ansi/escape :none)]]
    text))


(defn- delimiter
  "Colors a delimiter apropriately."
  [delim]
  (color-text delim :bold :red))



;; DISPATCH MULTIMETHOD

(defn- canonize-dispatch
  [value]
  (if (extends? data/TaggedValue (class value))
    :tagged-value
    (type value)))


(defmulti canonize
  "Converts the given value into a 'canonical' structured document, suitable
  for printing with fipp. This method also supports ANSI color escapes for
  syntax highlighting if desired."
  #'canonize-dispatch)


(defmacro ^:private color-primitive
  [dispatch & codes]
  `(defmethod canonize ~dispatch
     [value#]
     (color-text (pr-str value#) ~@codes)))


(color-primitive nil :bold :black)
(color-primitive java.lang.Boolean :green)
(color-primitive java.lang.Number :cyan)
(color-primitive java.lang.Character :bold :magenta)
(color-primitive java.lang.String :bold :magenta)
(color-primitive clojure.lang.Keyword :bold :yellow)
(color-primitive clojure.lang.Symbol)


(defmethod canonize clojure.lang.ISeq
  [s]
  (let [elements (if (symbol? (first s))
                   (cons (color-text (str (first s)) :bold :blue)
                         (map canonize (rest s)))
                   (map canonize s))]
    [:group
     (delimiter "(")
     [:align (interpose :line elements)]
     (delimiter ")")]))


(defmethod canonize clojure.lang.IPersistentVector
  [v]
  [:group
   (delimiter "[")
   [:align (interpose :line (map canonize v))]
   (delimiter "]")])


(defmethod canonize clojure.lang.IPersistentSet
  [s]
  (let [entries (sort data/total-order (seq s))]
    [:group
     (delimiter "#{")
     [:align (interpose :line (map canonize entries))]
     (delimiter "}")]))


(defn- canonize-map
  [m]
  (let [canonize-kv (fn [[k v]] [:span (canonize k) " " (canonize v)])
        entries (->> (seq m)
                     (sort-by first data/total-order)
                     (map canonize-kv))]
    [:group
     (delimiter "{")
     [:align (interpose [:span "," :line] entries)]
     (delimiter "}")]))


(defmethod canonize clojure.lang.IPersistentMap
  [m]
  (canonize-map m))


(defmethod canonize clojure.lang.IRecord
  [r]
  (if *strict-mode*
    (throw (IllegalArgumentException.
             (str "No canonical representation for " (class r) ": " r)))
    [:span (delimiter "#") (-> r class .getName) (canonize-map r)]))


(prefer-method canonize clojure.lang.IRecord clojure.lang.IPersistentMap)


(defmethod canonize :tagged-value
  [v]
  (vary-meta
    [:span (color-text (str \# (data/edn-tag v)) :red)
     " " (canonize (data/edn-value v))]
    assoc ::tagged-value true))


(defn tagged-value-doc?
  [doc]
  (::tagged-value (meta doc)))


(defmethod canonize :default
  [value]
  (if *strict-mode*
    (throw (IllegalArgumentException.
             (str "No canonical representation for " (class value) ": " value)))
    [:span (color-text "#<" :blue)
     (color-text (.getName (class value)) :bold :blue)
     " " (str value)
     (color-text ">" :blue)]))



;; PRINT FUNCTIONS

(defprinter pprint canonize {:width 80})


(defn cprint
  "Like pprint, but turns on colored output."
  ([value]
   (binding [*colored-output* true]
     (pprint value)))
  ([value opts]
   (binding [*colored-output* true]
     (pprint value opts))))
