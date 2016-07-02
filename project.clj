(defproject mvxcvi/puget "1.1.0-SNAPSHOT"
  :description "Colorizing canonical Clojure printer for EDN values."
  :url "https://github.com/greglook/puget"
  :license {:name "Public Domain"
            :url "http://unlicense.org/"}

  :deploy-branches ["master"]

  :plugins
  [[lein-cljfmt "0.5.3"]
   [lein-cloverage "1.0.6"]]

  :dependencies
  [[fipp "0.6.5"]
   [mvxcvi/arrangement "1.1.0"]
   [org.clojure/clojure "1.8.0"]]

  :cljfmt
  {:remove-consecutive-blank-lines? false
   :indents {ns [[:block 1] [:inner 1]]
             defrecord ^:replace [[:block 1] [:inner 1]]
             with-options [[:inner 0]]
             #"[^\[{].*" [[:block 0]]}}

  :codox
  {:metadata {:doc/format :markdown}
   :source-uri "https://github.com/greglook/puget/blob/master/{filepath}#L{line}"
   :doc-paths ["doc/extra"]
   :output-path "doc/api"}

  :hiera
  {:path "doc/ns-hiera.png"
   :cluster-depth 1})
