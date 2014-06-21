(ns puget.printer-test
  (:require
    [clojure.string :as str]
    [clojure.test :refer :all]
    (puget
      [data :refer [TaggedValue]]
      [printer :refer :all])))


#_
(deftest color-scheme-setting
  (let [old-scheme *color-scheme*]
    (set-color-scheme! {:tag [:green]})
    (is (= [:green] (:tag *color-scheme*)))
    (set-color-scheme! :nil [:black] :number [:bold :cyan])
    (is (= [:black] (:nil *color-scheme*)))
    (is (= [:bold :cyan] (:number *color-scheme*)))
    (set-color-scheme! old-scheme)))


#_
(deftest map-delimiter-setting
  (let [old-delim *map-delimiter*]
    (set-map-commas!)
    (is (= "," *map-delimiter*))
    (alter-var-root #'*map-delimiter* (constantly old-delim))))


(deftest canonical-primitives
  (testing "Primitive values"
    (are [v text] (= text (-> v pprint with-out-str str/trim))
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
    (are [v text] (= text (pprint-str v))
         '(foo :bar)            "(foo :bar)"
         '(1 2 3)               "(1 2 3)"
         [4 "five" 6.0]         "[4 \"five\" 6.0]"
         {:foo 8 :bar 'baz}     "{:bar baz :foo 8}" ; gets sorted
         #{:omega :alpha :beta} "#{:alpha :beta :omega}"))) ; also sorted


(defrecord TestRecord [foo bar])

(deftest canonical-records
  (testing "Records"
    (let [r (->TestRecord \x \y)]
      (with-strict-mode
        (is (thrown? IllegalArgumentException (pprint r))
            "should not print non-EDN representation"))
      (is (= (with-out-str (pprint r))
             "#puget.printer_test.TestRecord{:bar \\y :foo \\x}\n")))))


(deftest canonical-tagged-value
  (let [tval (reify TaggedValue
               (edn-tag [this] 'foo)
               (edn-value [this] :bar/baz))
        doc (canonize tval)]))


(deftest default-canonize
  (testing "Unknown values"
    (let [usd (java.util.Currency/getInstance "USD")]
      (with-strict-mode
        (is (thrown? IllegalArgumentException
                     (pprint usd))
                     "should not print non-EDN representation"))
      (is (= (with-out-str (pprint usd))
             "#<java.util.Currency USD>\n")))))


(deftest colored-printing
  (let [value [nil 1.0 true "foo" :bar]
        bw-str (with-out-str (pprint value))
        colored-str (cprint-str value)
        thin-str (cprint-str value {:width 5})]
    (is (> (count colored-str) (count bw-str)))
    (is (not= colored-str thin-str))
    (is (= "123" (with-color (color-text :frobble "123"))))
    (is (= "#{:baz}" (pprint-str #{:baz})))
    (is (= (cprint-str :foo)
           (with-color (color-text :keyword ":foo"))))))
