(ns puget.color.ansi-test
  (:require
   [clojure.test :refer :all]
   [puget.color.ansi :as ansi]))

(deftest colored-text
  (let [text "foo"
        color (ansi/sgr text :red)]
    (is (< (count text) (count color)))
    (is (= text (ansi/strip color)))))
