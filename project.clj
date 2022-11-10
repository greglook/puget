(defproject mvxcvi/puget "1.3.4"
  :description "Colorizing canonical Clojure printer for EDN values."
  :url "https://github.com/greglook/puget"
  :license {:name "Public Domain"
            :url "http://unlicense.org/"}

  :deploy-branches ["main"]
  :pedantic? :warn

  :plugins
  [[lein-cloverage "1.2.2"]]

  :dependencies
  [[org.clojure/clojure "1.11.1"]
   [mvxcvi/arrangement "2.1.0"]
   [fipp "0.6.26"]]

  :profiles
  {:clj-1.10
   {:dependencies [[org.clojure/clojure "1.10.3"]]}

   :clj-1.9
   {:dependencies [[org.clojure/clojure "1.9.0"]]}

   :clj-1.8
   {:dependencies [[org.clojure/clojure "1.8.0"]]}

   :clj-1.7
   {:dependencies [[org.clojure/clojure "1.7.0"]]}})
