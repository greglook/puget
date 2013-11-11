(ns puget.printer-test
  (:require
    [clojure.string :as string]
    [clojure.test :refer :all]
    (puget
      [data :as data]
      [printer :refer :all])))


(deftest canonical-primitives
  (testing "Primitive values"
    (are [v text] (= text (-> v pprint with-out-str string/trim))
         nil     "nil"
         true    "true"
         false   "false"
         0       "0"
         1234N   "1234N"
         2.718   "2.718"
         3.14M   "3.14M"
         3/10    "3/10"
         \a      "\\a"
         \space  "\\space"
         "foo"   "\"foo\""
         :key    ":key"
         :ns/key ":ns/key"
         'sym    "sym"
         'ns/sym "ns/sym")))


(deftest canonical-collections
  (testing "Collections"
    (are [v text] (= text (-> v pprint with-out-str string/trim))
         '(foo :bar)            "(foo :bar)"
         '(1 2 3)               "(1 2 3)"
         [4 "five" 6.0]         "[4 \"five\" 6.0]"
         {:foo 8 :bar 'baz}     "{:bar baz, :foo 8}" ; gets sorted
         #{:omega :alpha :beta} "#{:alpha :beta :omega}"))) ; also sorted


(defrecord TestRecord [foo bar])

(deftest canonical-records
  (testing "Records"
    (let [r (->TestRecord \x \y)]
      (binding [*strict-mode* true]
        (is (thrown? IllegalArgumentException (pprint r))
            "should not print non-EDN representation"))
      (is (= (with-out-str (pprint r))
             "#puget.printer_test.TestRecord{:bar \\y, :foo \\x}\n")))))


(deftest default-canonize
  (testing "Unknown values"
    (let [usd (java.util.Currency/getInstance "USD")]
      (binding [*strict-mode* true]
        (is (thrown? IllegalArgumentException
                     (pprint usd))
                     "should not print non-EDN representation"))
      (is (= (with-out-str (pprint usd))
             "#<java.util.Currency USD>\n")))))


(deftest colored-printing
  (let [value [nil 1.0 true "foo" :bar]
        bw-str (with-out-str (pprint value))
        colored-str (with-out-str (cprint value))]
    (is (> (count colored-str) (count bw-str)))))
