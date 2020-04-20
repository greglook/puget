Change Log
==========

All notable changes to this project will be documented in this file, which
follows the conventions of [keepachangelog.com](http://keepachangelog.com/).
This project adheres to [Semantic Versioning](http://semver.org/).


## [Unreleased]

...


## [1.2.1] - 2020-04-19

### Changed
- Minor version upgrades


## [1.2.0] - 2019-10-11

### Fixed
- Ansi codes now respect indentation and start at beginning of element rather
  than beginning of line.
  [#44](//github.com/greglook/puget/issues/44)

### Changed
- Update CI configuration to support style checks and separate coverage check.
- Switch to cljdoc for documentation.


## [1.1.2] - 2019-04-04

### Fixed
- The `:html-inline` color markup now properly handles nested documents in
  `color/document` calls.
- The `:html-inline` color markup now defaults to assuming a style value is a
  color, allowing use of any CSS color name.


## [1.1.1] - 2019-03-17

### Changed
- Upgrade to clojure 1.10.0.
- Upgrade to clj-arrangement 1.2.0 for a ~3x speedup when sorting complex
  collections for canonical printing.


## [1.1.0] - 2018-12-16

### Added
- The pretty-printer supports a `:namespace-maps` option which will render maps
  using the new namespace map literal syntax introduced in Clojure 1.9.0 when
  possible.
  [#36](//github.com/greglook/puget/issues/36)

### Changed
- Collections which are `sorted?` are no longer reordered by the pretty-printer.
  [#37](//github.com/greglook/puget/issues/37)
- Empty collections now omit child document nodes, so they are slightly more
  efficient to render.


## [1.0.3] - 2018-10-30

### Changed
- Upgrade to fipp 0.6.13 for Java 11 compatibility.
  [#38](//github.com/greglook/puget/pull/38)


## [1.0.2] - 2017-11-04

### Changed
- Upgrade to CircleCI 2.0
- Upgrade various minor dependency versions


## [1.0.1] - 2016-08-23

### Added
- Add checks to `printer/tagged-handler` to ensure argumenst are a symbol and
  function, respectively.

### Changed
- Upgraded fipp to 0.6.6
  [#33](//github.com/greglook/puget/pull/33)


## [1.0.0] - 2015-10-29

Finally decided it was time to recognize that this is a stable library!

### Added
- Adopt `visit-record` logic from upgrade to fipp 0.6.3.
  [brandonbloom/fipp#29](//github.com/brandonbloom/fipp/pull/29)

### Changed
- Move pattern and var printing back into `PrettyPrinter` visitor protocol.
- Improve class name parsing of inner function names.


## [0.9.2] - 2015-10-20

### Added
- Add `printer/unknown-handler` and improve rendering of unknown types.
- Add print handlers for class and function values.

### Changed
- Line-break tagged literals when the form is a collection.


## [0.9.1] - 2015-10-17

### Added
- Add `printer/pr-handler` as a useful default for dispatching.
- Add `:seq-limit` option, to cap realization of lazy sequences.

### Changed
- Improve `chained-lookup` logic to remove nil dispatch entries.
- Rename `:sort-mode` option back to `:sort-keys`.
  [#25](//github.com/greglook/puget/issues/25)

### Fixed
- Fix rendering of lazy sequences.
  [#26](//github.com/greglook/puget/issues/26)


## [0.9.0] - 2015-10-13

This version involves a major update to the printer internals with the upgrade
to fipp and Clojure 1.7.

### Added
- Add more `:print-fallback` possibilities.
- Add `CanonicalPrinter` as a minimalist alternative to `PrettyPrinter`.

### Changed
- Upgrade to Clojure 1.7 and fipp 0.6.2.
- Separate total-order comparator into
  [mvxcvi/arrangement](https://github.com/greglook/clj-arrangement).
- Reorder args to `color/document` and `color/text`.
- Drop `puget.data/ExtendedNotation` protocol and switched to type-dispatched
  print-handler approach.
- Rename `:sort-keys` option to `:sort-mode`.

### Removed
- Remove `:escape-types` functionality in favor of `:print-handlers`.


## [0.8.1] - 2015-04-25

### Removed
- Remove `tagged-literal` code which conflicts with the built-in `clojure.core`
  functions in 1.7. [#20](//github.com/greglook/puget/issues/20)


## [0.8.0] - 2015-03-10

### Added
- Add `:escape-types` option to avoid rendering types that Puget does not handle
  well. [#19](//github.com/greglook/puget/pull/19)

### Removed
- Remove byte array and URI extension to Whidbey.
  [#16](//github.com/greglook/puget/issue/16)


## [0.7.1] - 2015-02-28

### Added
- Add `:print-fallback` option to support custom `print-method` implementations.
  [#18](//github.com/greglook/puget/pull/18)

### Changed
- Refactor document formatting multimethod to dispatch on `type` metadata.
  [#13](//github.com/greglook/puget/issue/13)
  [#14](//github.com/greglook/puget/pull/14)
- Change `TaggedValue` to `TaggedLiteral` per the discussion
  [here](https://groups.google.com/forum/#!topic/clojure-dev/LW0ocQ1RcYI).
- Change color markup to a customizable multimethod with ANSI and HTML output.
  [#15](//github.com/greglook/puget/pull/15)

### Fixed
- Update documentation and clean up docstrings.


## [0.6.6] - 2014-12-28

### Added
- Add `:sort-keys` option to allow bounded canonicalization.
  [#12](//github.com/greglook/puget/pull/12)


## [0.6.4] - 2014-09-23

### Added
- Add `merge-options` and `with-options` helpers.
- Add `:coll-separator` option.
- Add format method for `IDeref` and `IPending` references.
  [#9](//github.com/greglook/puget/issues/9)
  [#10](//github.com/greglook/puget/pull/10)

### Changed
- Maps default to using commas between entries.
- Gave characters a separate syntax element than strings.


[Unreleased]: https://github.com/greglook/puget/compare/1.2.1...HEAD
[1.2.1]: https://github.com/greglook/puget/compare/1.2.0...1.2.1
[1.2.0]: https://github.com/greglook/puget/compare/1.1.2...1.2.0
[1.1.2]: https://github.com/greglook/puget/compare/1.1.1...1.1.2
[1.1.1]: https://github.com/greglook/puget/compare/1.1.0...1.1.1
[1.1.0]: https://github.com/greglook/puget/compare/1.0.3...1.1.0
[1.0.3]: https://github.com/greglook/puget/compare/1.0.2...1.0.3
[1.0.2]: https://github.com/greglook/puget/compare/1.0.1...1.0.2
[1.0.1]: https://github.com/greglook/puget/compare/1.0.0...1.0.1
[1.0.0]: https://github.com/greglook/puget/compare/0.9.2...1.0.0
[0.9.2]: https://github.com/greglook/puget/compare/0.9.1...0.9.2
[0.9.1]: https://github.com/greglook/puget/compare/0.9.0...0.9.1
[0.9.0]: https://github.com/greglook/puget/compare/0.8.1...0.9.0
[0.8.1]: https://github.com/greglook/puget/compare/0.8.0...0.8.1
[0.8.0]: https://github.com/greglook/puget/compare/0.7.1...0.8.0
[0.7.1]: https://github.com/greglook/puget/compare/0.6.6...0.7.1
[0.6.6]: https://github.com/greglook/puget/compare/0.6.4...0.6.6
[0.6.4]: https://github.com/greglook/puget/compare/0.6.3...0.6.4
