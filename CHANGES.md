Release Notes
=============

This page documents the high-level changes in each release of Puget.

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
