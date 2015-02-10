(ns puget.color
  "This namespace defines multimethods to add color markup to text.")

;; ## Coloring Multimethods

(defn dispatch
  "Dispatches to coloring multimethods. Element should be a key from
  the color-scheme map."
  [element text options]
  (when (:print-color options)
    (:color-markup options)))


(defmulti document
  "Constructs a pretty print document, which may be colored if
  `:print-color` is true."
  #'dispatch)


(defmulti text
  "Produces text colored according to the active color scheme. This is mostly
  useful to clients which want to produce output which matches data printed by
  Puget, but which is not directly printed by the library. Note that this
  function still obeys the `:print-color` option."
  #'dispatch)


;; ## No markup when colorless

(defmethod document nil
  [element text options]
  text)


(defmethod text nil
  [element text options]
  text)
