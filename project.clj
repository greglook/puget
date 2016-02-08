(defproject mvxcvi/puget "1.1.0-SNAPSHOT"
  :description "Colorizing canonical Clojure printer for EDN values."
  :url "https://github.com/greglook/puget"
  :license {:name "Public Domain"
            :url "http://unlicense.org/"}

  :deploy-branches ["master"]

  :plugins
  [[lein-cloverage "1.0.6"]]

  :dependencies
  [[fipp "0.6.4"]
   [mvxcvi/arrangement "1.0.0"]
   [org.clojure/clojure "1.7.0"]]

  :codox {:metadata {:doc/format :markdown}
          :source-uri "https://github.com/greglook/puget/blob/master/{filepath}#L{line}"
          :doc-paths ["doc/extra"]
          :output-path "doc/api"}

  :hiera {:path "doc/ns-hiera.png"
          :cluster-depth 1})
