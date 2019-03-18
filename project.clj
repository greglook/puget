(defproject mvxcvi/puget "1.1.1-SNAPSHOT"
  :description "Colorizing canonical Clojure printer for EDN values."
  :url "https://github.com/greglook/puget"
  :license {:name "Public Domain"
            :url "http://unlicense.org/"}

  :deploy-branches ["master"]
  :pedantic? :abort

  :plugins
  [[mvxcvi/lein-cljfmt "0.7.0-SNAPSHOT"]
   [lein-cloverage "1.1.0"]]

  :dependencies
  [[org.clojure/clojure "1.10.0"]
   [mvxcvi/arrangement "1.2.0"]
   [fipp "0.6.17"]]

  :cljfmt
  {:remove-consecutive-blank-lines? false
   :indents {with-options [[:inner 0]]}}

  :hiera
  {:path "doc/ns-hiera.png"
   :cluster-depth 1}

  :codox
  {:metadata {:doc/format :markdown}
   :source-uri "https://github.com/greglook/puget/blob/master/{filepath}#L{line}"
   :doc-paths ["doc/extra"]
   :output-path "target/doc/api"})
