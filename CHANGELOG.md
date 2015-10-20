Release Notes
=============

This page documents the high-level changes in each release of Puget.

## 0.9.2 (2015-10-20)

- Add `printer/unknown-handler` and improve rendering of unknown types.
- Add print handlers for class and function values.
- Line-break tagged literals when the form is a collection.

## 0.9.1 (2015-10-17)

- Improve `chained-lookup` logic to remove nil dispatch entries.
- Add `printer/pr-handler` as a useful default for dispatching.
- Rename `:sort-mode` option back to `:sort-keys`. (#25)
- Fix rendering of lazy sequences. (#26)
- Add `:seq-limit` option, to cap realization of lazy sequences.

## 0.9.0 (2015-10-13)

- Upgrade to Clojure 1.7 and fipp 0.6.2.
- Separate total-order comparator into
  [mvxcvi/arrangement](https://github.com/greglook/clj-arrangement).
- Reordered args to `color/document` and `color/text`.
- Drop `puget.data/ExtendedNotation` protocol and switched to type-dispatched
  print-handler approach.
- Rename `:sort-keys` option to `:sort-mode`.
- Remove `:escape-types` functionality in favor of `:print-handlers`.
- Add more `:print-fallback` possibilities.
- Add `CanonicalPrinter` as a minimalist alternative to `PrettyPrinter`.

## 0.8.1 (2015-04-25)

- Remove `tagged-literal` code which conflicts with the built-in `clojure.core`
  functions in 1.7. [#20](//github.com/greglook/puget/issues/20)

## 0.8.0 (2015-03-10)

- Remove byte array and URI extension to Whidbey.
  [#16](//github.com/greglook/puget/issue/16)
- Added `:escape-types` option to avoid rendering types that Puget does not
  handle well.
  [#19](//github.com/greglook/puget/pull/19)

## 0.7.1 (2015-02-28)

- Updated documentation and clean up docstrings.
- Refactored document formatting multimethod to dispatch on `type` metadata.
  [#13](//github.com/greglook/puget/issue/13)
  [#14](//github.com/greglook/puget/pull/14)
- Changed `TaggedValue` to `TaggedLiteral` per the discussion
  [here](https://groups.google.com/forum/#!topic/clojure-dev/LW0ocQ1RcYI).
- Change color markup to a customizable multimethod with ANSI and HTML output.
  [#15](//github.com/greglook/puget/pull/15)
- Added `:print-fallback` option to support custom `print-method` implementations.
  [#18](//github.com/greglook/puget/pull/18)

## 0.6.6 (2014-12-28)

- Added `:sort-keys` option to allow bounded canonicalization. #12

## 0.6.4 (2014-09-23)

- Added `merge-options` and `with-options` helpers.
- Added `:coll-separator` option.
- Maps default to using commas between entries.
- Gave characters a separate syntax element than strings.
- Added format method for `IDeref` and `IPending` references.
  [#9](//github.com/greglook/puget/issues/9)
  [#10](//github.com/greglook/puget/pull/10)
