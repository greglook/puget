(defproject mvxcvi/puget "0.8.0-SNAPSHOT"
  :description "Colorizing canonical Clojure printer for EDN values."
  :url "https://github.com/greglook/puget"
  :license {:name "Public Domain"
            :url "http://unlicense.org/"}

  :deploy-branches ["master"]

  :aliases {"docs" ["do" ["doc"] ["marg" "--dir" "doc/marginalia"] ["hiera"]]
            "tests" ["do" ["check"] ["test"] ["cloverage"]]}

  :plugins [[codox "0.8.10"]
            [lein-cloverage "1.0.2"]
            [lein-marginalia "0.8.0"]]

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/data.codec "0.1.0"]
                 [fipp "0.5.2"]]

  :cljfmt {:indents {with-options [[:block 1]]}}

  :codox {:defaults {:doc/format :markdown}
          :output-dir "doc/api"
          :src-dir-uri "https://github.com/greglook/puget/blob/master/"
          :src-linenum-anchor-prefix "L"}

  :hiera {:path "doc/ns-hiera.png"
          :cluster-depth 1})
