(ns puget.color.ansi-test
  (:require
    [clojure.string :as str]
    [clojure.test :refer :all]
    [puget.color.ansi :as ansi]
    [puget.printer :as printer]))


(def c-delimiter       [:bold :red])
(def c-tag             [:red])
(def c-nil             [:bold :black])
(def c-boolean         [:green])
(def c-number          [:cyan])
(def c-string          [:bold :magenta])
(def c-character       [:magenta])
(def c-keyword         [:bold :yellow])
(def c-symbol          nil)
(def c-function-symbol [:bold :blue])
(def c-class-delimiter [:blue])
(def c-class-name      [:white])


(def test-color-scheme
  {:delimiter       c-delimiter
   :tag             c-tag
   :nil             c-nil
   :boolean         c-boolean
   :number          c-number
   :string          c-string
   :character       c-character
   :keyword         c-keyword
   :symbol          c-symbol
   :function-symbol c-function-symbol
   :class-delimiter c-class-delimiter
   :class-name      c-class-name})


(defn- escape-ansi
  "In the name of testability, split 's' into segments and replace ESC[1;23]m with printable ⦕1;23⦖."
  [s]
  (-> s
      (str/replace #"\u001b\[([0-9;]*)[mK]" "፨⦕$1⦖፨")
      (str/split #"፨")
      (->> (filter seq))))


(defn- ansi
  [text codes]
  (apply ansi/sgr text codes))


(deftest colored-text
  (let [text "foo"
        color (ansi/sgr text :red)]
    (is (< (count text) (count color)))
    (is (= text (ansi/strip color)))))


(deftest ansi-example-test
  (let [test-data [nil true \space "string"
                   {:omega 123N :alpha '(func x y) :gamma 3.14159}
                   #{\a "heterogeneous" :set}
                   #_(java.util.Currency/getInstance "USD")
                   (java.util.Date. 1570322071178)
                   (java.util.UUID/fromString "b537346e-8ad1-4bce-8bab-60fcd4007530")]]
    (is (= (escape-ansi
             (str (ansi "[" c-delimiter)
                  (ansi "nil" c-nil)
                  "\n " (ansi "true" c-boolean)
                  "\n " (ansi "\\space" c-character)
                  "\n " (ansi "\"string\"" c-string)

                  "\n " (ansi "{" c-delimiter)
                  (ansi ":alpha" c-keyword)
                  " " (ansi "(" c-delimiter) (ansi "func" c-function-symbol) " x y" (ansi ")" c-delimiter)
                  ", " (ansi ":gamma" c-keyword) " " (ansi "3.14159" c-number)
                  ", " (ansi ":omega" c-keyword) " " (ansi "123N" c-number)
                  (ansi "}" c-delimiter)

                  "\n " (ansi "#{" c-delimiter)
                  (ansi "\\a" c-character)
                  " " (ansi "\"heterogeneous\"" c-string)
                  " " (ansi ":set" c-keyword)
                  (ansi "}" c-delimiter)

                  "\n " (ansi "#inst" c-tag)
                  " " (ansi "\"2019-10-06T00:34:31.178-00:00\"" c-string)

                  "\n " (ansi "#uuid" c-tag)
                  " " (ansi "\"b537346e-8ad1-4bce-8bab-60fcd4007530\"" c-string)
                  (ansi "]" c-delimiter)))
           (escape-ansi
             (printer/cprint-str test-data
                                 {:color-markup :ansi
                                  :color-scheme test-color-scheme}))))))


(deftest ansi-nested-test
  (let [test-data ["item1"
                   ["item2"
                    ["item3"
                     ["item4"
                      ["item5"
                       ["item6"
                        ["item7"
                         ["item8"
                          ["item9"
                           ["item10"
                            ["item11"
                             ["item12"
                              ["item13"
                               ["item14"
                                ["item15"]]]]]]]]]]]]]]]]
    (is (= (escape-ansi
             (str (ansi "[" c-delimiter)
                  (ansi "\"item1\"" c-string)
                  "\n " (ansi "[" c-delimiter) (ansi "\"item2\"" c-string)
                  "\n  " (ansi "[" c-delimiter) (ansi "\"item3\"" c-string)
                  "\n   " (ansi "[" c-delimiter) (ansi "\"item4\"" c-string)
                  "\n    " (ansi "[" c-delimiter) (ansi "\"item5\"" c-string)
                  "\n     " (ansi "[" c-delimiter) (ansi "\"item6\"" c-string)
                  "\n      " (ansi "[" c-delimiter) (ansi "\"item7\"" c-string)
                  "\n       " (ansi "[" c-delimiter) (ansi "\"item8\"" c-string)
                  "\n        " (ansi "[" c-delimiter) (ansi "\"item9\"" c-string)
                  "\n         " (ansi "[" c-delimiter) (ansi "\"item10\"" c-string)
                  " " (ansi "[" c-delimiter) (ansi "\"item11\"" c-string)
                  " " (ansi "[" c-delimiter) (ansi "\"item12\"" c-string)
                  " " (ansi "[" c-delimiter) (ansi "\"item13\"" c-string)
                  " " (ansi "[" c-delimiter) (ansi "\"item14\"" c-string)
                  " " (ansi "[" c-delimiter) (ansi "\"item15\"" c-string)
                  (apply str (repeat 15 (ansi "]" c-delimiter)))))
           (escape-ansi
             (printer/cprint-str test-data
                                 {:color-markup :ansi
                                  :color-scheme test-color-scheme}))))))
