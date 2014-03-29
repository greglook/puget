Puget
=====

Puget is a Clojure library for printing [EDN](https://github.com/edn-format/edn)
values. Under the hood, Puget uses the
[Fast Idiomatic Pretty-Printer](https://github.com/brandonbloom/fipp) library to
format values into a _print document_. Puget offers two main features which set
it apart from FIPP and Clojure's native pretty-printing functions.

## ANSI Coloring

Puget's first main feature is colorizing the printed data using ANSI escape
codes. This is kind of like syntax highlighting, except much easier since the
code works directly with the data instead of parsing it from text.

Different syntax elements are given different colors to make reading the
printed output much easier for humans. The `*colored-output*` var can be set to
enable colorization - alternately, the `cprint` function prints with colored
output enabled:

![colorization example](screenshot.png)

## Canonical Representation

Puget's other main goal is to provide _canonical serialization_ of data. In
short, if two data values are equal, they should be printed identically. This is
important for data deduplication and in situations where the printed data is
hashed.

Towards this end, Puget defines a _total ordering_ on Clojure values, meaning
that it provides a comparator that can provide an ordering for any two values,
even if they have different types. This ordering is used to sort the values in
sets and the keys in maps so that they are always printed the same way.

By default, values with types which have no canonical representation defined
will be printed in the same style as Clojure's pretty-print. The `*strict-mode*`
var can be bound to true to throw an exception for these values instead.

```clojure
(require '[puget.printer :as puget])

(def usd (java.util.Currency/getInstance "USD"))

(puget/pprint usd)
; #<java.util.Currency USD>

(binding [puget/*strict-mode* true]
  (puget/pprint usd))
; IllegalArgumentException: No canonical representation for class java.util.Currency: USD
```

## EDN Tagged Values

All of Clojure's primitive types are given their standard canonical print
representations. To handle non-standard data types, EDN specifies _tags_ which
can alter how the reader interprets the following value. Accordingly, Puget
provides a `TaggedValue` protocol in the `puget.data` namespace. This lets novel
datatypes provide a 'canonical' representation of themselves as a tag symbol
followed by some interpretation of the value.

Puget extends this protocol to support the `#inst` and `#uuid` built-ins from
the EDN standard. In addition, it supports `#bin` for base64-encoded binary
data, and `#uri` for specifying Uniform Resource Identifiers.

## Installation

Puget is [published on Clojars](https://clojars.org/mvxcvi/puget).

To use this version with Leiningen, add the following dependency to your project
definition:

```clojure
[mvxcvi/puget "0.3.0"]
```

## License

This is free and unencumbered software released into the public domain.
See the UNLICENSE file for more information.
