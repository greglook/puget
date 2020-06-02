(defproject mvxcvi/puget "1.3.1"
  :description "Colorizing canonical Clojure printer for EDN values."
  :url "https://github.com/greglook/puget"
  :license {:name "Public Domain"
            :url "http://unlicense.org/"}

  :deploy-branches ["master"]
  :pedantic? :abort

  :plugins
  [[lein-cloverage "1.1.2"]]

  :dependencies
  [[org.clojure/clojure "1.10.1"]
   [mvxcvi/arrangement "1.2.0"]
   [fipp "0.6.23"]])
