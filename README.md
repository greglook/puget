Puget
=====

[![Build Status](https://travis-ci.org/greglook/puget.svg?branch=develop)](https://travis-ci.org/greglook/puget)
[![Coverage Status](https://coveralls.io/repos/greglook/puget/badge.png?branch=develop)](https://coveralls.io/r/greglook/puget?branch=develop)
[![Dependency Status](https://www.versioneye.com/user/projects/53718cc914c1586649000048/badge.png)](https://www.versioneye.com/clojure/mvxcvi:puget/0.7.0-SNAPSHOT)

Puget is a Clojure library for printing [EDN](https://github.com/edn-format/edn)
values. Under the hood, Puget formats data into a _print document_ and uses the
[Fast Idiomatic Pretty-Printer](https://github.com/brandonbloom/fipp) library to
render it. Puget offers two main features which set it apart from FIPP and
Clojure's native pretty-printing functions: [syntax coloring](#syntax-coloring)
and [canonical printing](#canonical-representation).

## Installation

Puget releases are published on Clojars. To use the latest version with
Leiningen, add the following dependency to your project definition:

[![Clojars Project](http://clojars.org/mvxcvi/puget/latest-version.svg)](http://clojars.org/mvxcvi/puget)

See [Whidbey](https://github.com/greglook/whidbey) for nREPL and Leiningen integration.

## Syntax Coloring

Puget's first main feature is colorizing the printed data using ANSI escape
codes or HTML `span` elements for color markup. This is kind of like syntax
highlighting, except much easier since the code works directly with the data
instead of parsing it from text.

Different syntax elements are given different colors to make reading the
printed output much easier for humans. The `:print-color` option can be set to
enable colorization using the `with-color` macro - alternately, the `cprint`
function always prints with colored output enabled:

![colorization example](screenshot.png)

The `:color-markup` option defaults to `:ansi`, but can be set to `:html-inline`
or `:html-classes` to use HTML `span` elements for color markup:

  - `:html-inline` uses inline styles to apply style attributes directly to
    each `span`'s content based on the `:color-scheme`;
  - `:html-classes` sets the `class` of each `span` based on its syntax element
    type (e.g., "delimiter", "keyword", "number") to allow the style for its
    content be specified elsewhere via CSS.

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
will be printed in the same style as Clojure's pretty-print. In strict mode,
Puget will throw an exception for these values instead.

```clojure
(require '[puget.printer :as puget])

(def usd (java.util.Currency/getInstance "USD"))

(puget/pprint usd)
;; #<java.util.Currency@4cc4ee24 USD>

(puget/pprint usd {:strict true})
;; IllegalArgumentException: No canonical representation for class java.util.Currency: USD
```

Whether or not the entries in collections are sorted can be controlled with the
`:sort-keys` option.

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
  (partial f/unparse (f/formatters :date-time)))

(t/now)
#inst "2014-05-14T01:05:53.885Z"
```

## Further Customization

Puget's printing is controlled by a map of options which include print width,
the color scheme, whether to be strict about value representations, whether to
print metadata, etc. The default options are held in the dynamic var
`puget.printer/*options*`. This can be bound with the `with-options` macro for
convenience, or a map can be passed directly into Puget's print functions to
override the defaults.

Puget's colors are defined by the `:color-scheme` key, which maps syntax element
keywords to a vector of ANSI style keywords to apply.  The `set-color-scheme!`
function offers a convenient way to change the colors by providing either
color/style argument pairs or a single map of colors to merge into the current
color scheme.

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
 :character [:bold :magenta]
 :symbol nil
 :tag [:red]}
```

## License

This is free and unencumbered software released into the public domain.
See the UNLICENSE file for more information.
