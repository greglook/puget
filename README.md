Puget
=====

Puget is a Clojure library for printing [EDN](https://github.com/edn-format/edn)
values. Under the hood, Puget uses the
[Fast Idiomatic Pretty-Printer](https://github.com/brandonbloom/fipp) library to
format values into a _print document_. Puget offers two main features which set
it apart from FIPP or Clojure's native pretty-printing functions.

## Canonical Representation

One of Puget's goals is to provide _canonical serialization_ of data. In short,
if two data values are equal, they should be printed identically. This is
important for data deduplication and in situations where the printed data is
hashed.

All of Clojure's primitive types are given their standard canonical print
representations. To handle non-standard data types, EDN specifies _tags_ which
can alter how the reader interprets the following value. Accordingly, Puget
provides a `TaggedValue` protocol in the `puget.data` namespace. This lets novel
datatypes provide a 'canonical' representation of themselves as a tag symbol
followed by some interpretation of the value.

Puget extends this protocol to support the `#inst` and `#uuid` built-ins from
the EDN standard. In addition, it supports `#bin` for base64-encoded binary
data, and `#uri` for specifying Uniform Resource Identifiers.

By default, values with types which have no canonical representation defined
will be printed in the same style as Clojure's pretty-print. The
`puget.printer/*strict-mode*` var can be bound to true to throw an exception for
these values instead.

```clojure
(require '[puget.printer :as puget])

(def usd (java.util.Currency/getInstance "USD"))

(puget/pprint usd)
; #<java.util.Currency USD>

(binding [puget/*strict-mode* true]
  (puget/pprint usd))
; IllegalArgumentException No canonical representation for class java.util.Currency: USD
```

## ANSI Coloring

Puget's second main feature is colorizing the printed data using ANSI escape
codes.  This is kind of like reverse syntax highlighting; different types are
given different colors to make reading the printed output much easier for
humans. The `puget.printer/*colored-output*` var can be set to enable
colorization - alternately, the `cprint` function prints with colored output.

```clojure
(require '[puget.printer :refer [cprint]])

(cprint {:foo "bar" :baz 1})
; {:baz 1 :foo "bar"}
```

## License

This is free and unencumbered software released into the public domain.
See the UNLICENSE file for more information.
