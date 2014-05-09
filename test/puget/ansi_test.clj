(ns puget.ansi-test
  (:require
    [clojure.test :refer :all]
    [puget.ansi :as ansi]))


(deftest colored-text
  (let [text "foo"
        color (ansi/sgr text :red)]
    (is (< (count text) (count color)))
    (is (= text (ansi/strip color)))))
