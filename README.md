Puget
=====

[![Build Status](https://travis-ci.org/greglook/puget.svg?branch=master)](https://travis-ci.org/greglook/puget)
[![Coverage Status](https://coveralls.io/repos/greglook/puget/badge.png?branch=master)](https://coveralls.io/r/greglook/puget?branch=master)
[![Dependency Status](https://www.versioneye.com/user/projects/53718bfb14c1589a89000144/badge.png?style=flat)](https://www.versioneye.com/clojure/mvxcvi:puget)
[![API codox](http://b.repl.ca/v1/doc-API-blue.png)](https://greglook.github.io/puget/api/)
[![marginalia docs](http://b.repl.ca/v1/doc-marginalia-blue.png)](https://greglook.github.io/puget/marginalia/uberdoc.html)

Puget is a Clojure library for printing Clojure and
[EDN](https://github.com/edn-format/edn) values. Under the hood, Puget formats
data into a _print document_ and uses the [Fast Idiomatic
Pretty-Printer](https://github.com/brandonbloom/fipp) library to render it.

Puget offers two main features which set it apart from FIPP and Clojure's native
pretty-printing functions: [syntax coloring](#syntax-coloring) and [canonical
printing](#canonical-representation). Custom rendering is supported using type
dispatch to select [print handlers](#type-extensions).

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
or `:html-classes` to use HTML `span` elements for color markup. Finally, the
`:color-scheme` map controls how various elements are highlighted.

## Canonical Representation

Puget's other main goal is to provide _canonical serialization_ of data. In
short, if two data values are equal, they should be printed identically. This is
important for data deduplication and in situations where the printed data is
hashed.

By default, Puget uses the
[arrangement](https://github.com/greglook/clj-arrangement) library to sort the
values in sets and the keys in maps so they are always printed the same way.
This can be disabled with the `:sort-mode` option, or enabled only for
collections under a certain size.

Most printing is done with the `puget.printer.PrettyPrinter` class, but the
library also offers the `CanonicalPrinter` for serializing data in a stricter
(and more compact) fashion.

```clojure
=> (require '[puget.printer :as puget])

=> (puget/pprint #{'x :a :z 3 1.0})
#{1.0 3 :a :z x}

=> (def usd (java.util.Currency/getInstance "USD"))
#'user/usd

=> (puget/pprint usd)
#<java.util.Currency@4cc4ee24 USD>

=> (puget/render-out (puget/canonical-printer) usd)
; IllegalArgumentException: No defined representation for class java.util.Currency: USD
```

## Type Extensions

All of Clojure's primitive types are given their standard canonical print
representations. To handle non-standard data types, Puget supports a mechanism
to dispatch to custom _print handlers_. These take precedence over the standard
rendering mechanisms.

This can be used to provide an EDN tagged-literal representation for certain
types, or just avoid trying to pretty-print types which the engine struggles
with (such as attempting to render a Datomic database).

The `puget.dispatch` namespace has functions to help build handler lookup
functions; most commonly, a chained inheritance-based lookup provides semantics
similar to Clojure's multimethod dispatch.

As an example, extending `#inst` formatting to clj-time's `DateTime`:

```clojure
=> (require '[clj-time.core :as t]
            '[clj-time.format :as f])

=> (puget/pprint (t/now))
#<org.joda.time.DateTime 2014-05-14T00:58:40.922Z>

=> (def time-handlers
     {org.joda.time.DateTime
      (puget/tagged-handler
        'inst
        (partial f/unparse (f/formatters :date-time)))}})
#'user/time-handlers

=> (puget/pprint (t/now) {:print-handlers time-handlers})
#inst "2014-05-14T01:05:53.885Z"
```

If no handler is specified for a given type and it's not a built-in EDN type,
Puget refers to the `:print-fallback` option. The default `:pretty` prints a
colored representation of the unknown value (note this is not valid EDN!),
while `:print` will fall back to the standard `pr-str`. Alternately, `:error`
will throw an exception for types with no defined representation. Finally, a
function may be provided which will be passed the current printer and the
unknown value to render.

## Further Customization

Puget's printing is controlled by a map of options which include print width,
sorting mode, color scheme and style, whether to print metadata, etc. The
default options are held in the dynamic var `puget.printer/*options*`. This can
be bound with the `with-options` macro for convenience, or a map can be passed
directly into Puget's print functions to override the defaults.

These options are used to construct a printer record, which is either the
`PrettyPrinter` or `CanonicalPrinter`. The printers can be used directly to
render values with `render-out` or `render-str` if maximal repeatability is
desired.

## License

This is free and unencumbered software released into the public domain.
See the UNLICENSE file for more information.
