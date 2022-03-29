(defproject mvxcvi/puget "1.3.2"
  :description "Colorizing canonical Clojure printer for EDN values."
  :url "https://github.com/greglook/puget"
  :license {:name "Public Domain"
            :url "http://unlicense.org/"}

  :deploy-branches ["main"]
  :pedantic? :abort

  :plugins
  [[lein-cloverage "1.2.2"]]

  :dependencies
  [[org.clojure/clojure "1.11.0"]
   [mvxcvi/arrangement "2.0.0"]
   [fipp "0.6.25"]])
