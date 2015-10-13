(defproject mvxcvi/puget "0.10.0-SNAPSHOT"
  :description "Colorizing canonical Clojure printer for EDN values."
  :url "https://github.com/greglook/puget"
  :license {:name "Public Domain"
            :url "http://unlicense.org/"}

  :deploy-branches ["master"]

  :plugins
  [[lein-cloverage "1.0.6"]]

  :dependencies
  [[fipp "0.6.2"]
   [mvxcvi/arrangement "1.0.0"]
   [org.clojure/clojure "1.7.0"]]

  :cljfmt {:indents {with-options [[:block 1]]}}

  :codox {:defaults {:doc/format :markdown}
          :output-dir "doc/api"
          :src-dir-uri "https://github.com/greglook/puget/blob/master/"
          :src-linenum-anchor-prefix "L"}

  :hiera {:path "doc/ns-hiera.png"
          :cluster-depth 1})
