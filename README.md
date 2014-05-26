Puget
=====

[![Build Status](https://travis-ci.org/greglook/puget.svg?branch=master)](https://travis-ci.org/greglook/puget)
[![Coverage Status](https://coveralls.io/repos/greglook/puget/badge.png?branch=master)](https://coveralls.io/r/greglook/puget?branch=master)
[![Dependency Status](https://www.versioneye.com/user/projects/53718bfb14c1589a89000144/badge.png)](https://www.versioneye.com/clojure/mvxcvi:puget/0.5.2)

Puget is a Clojure library for printing [EDN](https://github.com/edn-format/edn)
values. Under the hood, Puget formats data into a _print document_ and uses the
[Fast Idiomatic Pretty-Printer](https://github.com/brandonbloom/fipp) library to
render it. Puget offers two main features which set it apart from FIPP and
Clojure's native pretty-printing functions: [syntax coloring](#syntax-coloring)
and [canonical printing](#canonical-representation).

## Installation

Puget releases are [published on Clojars](https://clojars.org/mvxcvi/puget).

To use this version with Leiningen, add the following dependency to your project
definition:

```clojure
[mvxcvi/puget "0.5.2"]
```

## Syntax Coloring

Puget's first main feature is colorizing the printed data using ANSI escape
codes. This is kind of like syntax highlighting, except much easier since the
code works directly with the data instead of parsing it from text.

Different syntax elements are given different colors to make reading the
printed output much easier for humans. The `*colored-output*` var can be set to
enable colorization using the `with-color` macro - alternately, the `cprint`
function prints with colored output enabled:

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
;; #<java.util.Currency USD>

(binding [puget/*strict-mode* true]
  (puget/pprint usd))
;; IllegalArgumentException: No canonical representation for class java.util.Currency: USD
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

To give your own types a tag extension, use the `extend-tagged-*` functions. For
example, to extend `#inst` tagging to Joda `DateTime` objects:

```clojure
(require '(clj-time [core :as t] [format :as f]))

(t/now)
#<org.joda.time.DateTime 2014-05-14T00:58:40.922Z>

(require 'puget.data)

(puget.data/extend-tagged-value
  org.joda.time.DateTime 'inst
  (partial f/unparse (ftime/formatters :date-time)))

(t/now)
#inst "2014-05-14T01:05:53.885Z"
```

## Customization

Puget's colors are defined in the `*color-scheme*` var, which maps syntax
element keywords to a vector of ANSI style keywords to apply. The
`set-color-scheme!` function offers a convenient way to change the color scheme
by providing either color/style argument pairs or a single map of colors to
merge into the current color scheme.

```clojure
(puget/set-color-scheme! :nil [:bold :black])
{:boolean [:green]
 :class-delimiter [:blue]
 :class-name [:bold :blue]
 :delimiter [:bold :red]
 :function-symbol [:bold :blue]
 :keyword [:bold :yellow]
 :nil [:bold :black]
 :number [:cyan]
 :string [:bold :magenta]
 :symbol nil
 :tag [:red]}
```

By default, Puget does not put any delimiters between map entries. This is
controlled by the `*map-delimiter*` var. For convenience, Puget provides a
function to change the delimiter to a comma instead:

```clojure
(def value {:z 'qx :a 123})

(puget/pprint value)
;; {:a 123 :z qx}

(puget/set-map-commas!)

(puget/pprint value)
;; {:a 123, :z qx}
```

## License

This is free and unencumbered software released into the public domain.
See the UNLICENSE file for more information.
