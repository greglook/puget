(ns puget.printer-test
  (:require
    [clojure.string :as str]
    [clojure.test :refer :all]
    [puget.printer :refer :all]))


;; ## Test Types

(defrecord TestRecord
  [foo bar])


(deftype APending
  [is-realized]

  clojure.lang.IDeref

  (deref [this] 1)


  clojure.lang.IPending

  (isRealized [this] is-realized))


(deftype ComplexValue
  []

  Object

  (toString [_] "to-string"))


(defmethod print-method ComplexValue
  [this w]
  (.write w "{{ complex value print }}"))



;; ## Canonical Printing

(deftest canonical-primitives
  (let [printer (canonical-printer)]
    (are [v text] (= text (render-str printer v))
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
  (let [printer (canonical-printer)]
    (are [v text] (= text (render-str printer v))
      '()                    "()"
      '(foo :bar)            "(foo :bar)"
      '(1 2 3)               "(1 2 3)"
      []                     "[]"
      [4 "five" 6.0]         "[4 \"five\" 6.0]"
      {}                     "{}"
      {:foo 8, :bar 'baz}    "{:bar baz :foo 8}"
      #{}                    "#{}"
      #{:omega :alpha :beta} "#{:alpha :beta :omega}"
      (lazy-seq [:x])        "(:x)"
      (map inc [0 1 2])      "(1 2 3)")))


(deftest canonical-metadata
  (let [printer (canonical-printer)
        value ^:foo [:bar]]
    (binding [*print-meta* true]
      (is (= "[:bar]" (render-str printer value))
          "should not render metadata"))))


(deftest canonical-extensions
  (testing "tagged-handler construction"
    (is (thrown? clojure.lang.ExceptionInfo (tagged-handler "foo" str)))
    (is (thrown? clojure.lang.ExceptionInfo (tagged-handler 'foo "abcd")))
    (is (ifn? (tagged-handler 'foo str))))
  (let [handlers {java.util.UUID (tagged-handler 'uuid str)}
        printer (canonical-printer handlers)
        uuid-str "31f7dd72-c7f7-4a15-a98b-0f9248d3aaa6"]
    (is (= (str "#uuid \"" uuid-str "\"")
           (render-str printer (java.util.UUID/fromString uuid-str))))))


(deftest canonical-errors
  (let [printer (canonical-printer)]
    (are [v] (thrown? IllegalArgumentException
               (render-str printer v))
      #"^foo"
      #'*options*
      (delay 5)
      (atom :foo)
      (->TestRecord :x \y)
      (java.util.Currency/getInstance "USD"))))



;; ## Pretty Printing

(deftest pretty-primitives
  (are [v text] (= text (pprint-str v))
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
    'ns/sym "ns/sym"))


(deftest pretty-collections
  (are [v text] (= text (pprint-str v))
    '()                    "()"
    '(foo :bar)            "(foo :bar)"
    '(1 2 3)               "(1 2 3)"
    []                     "[]"
    [4 "five" 6.0]         "[4 \"five\" 6.0]"
    {}                     "{}"
    {:foo 8 :bar 'baz}     "{:bar baz, :foo 8}"     ; gets sorted
    #{}                    "#{}"
    #{:omega :alpha :beta} "#{:alpha :beta :omega}" ; also sorted
    (lazy-seq [:x])        "(:x)"))


(deftest pretty-java-types
  (testing "class types"
    (is (re-seq #"#<Class@[0-9a-f]+ java\.util\.Date>"
                (pprint-str java.util.Date))))
  (testing "regex"
    (let [v #"\d+"]
      (is (= "#\"\\d+\"" (pprint-str v)))))
  (testing "date instants"
    (is (= "#inst \"2015-10-12T05:23:08.000-00:00\""
           (pprint-str (java.util.Date. 1444627388000)))))
  (testing "UUIDs"
    (let [uuid-str "31f7dd72-c7f7-4a15-a98b-0f9248d3aaa6"]
      (is (= (str "#uuid \"" uuid-str "\"")
             (pprint-str (java.util.UUID/fromString uuid-str)))))))


(deftest pretty-clojure-types
  (testing "records"
    (let [r (->TestRecord \x \y)]
      (is (= "#puget.printer_test.TestRecord\n{:bar \\y, :foo \\x}"
             (pprint-str r {:width 30}))
          "long record prints with form on new line")
      (is (= "#puget.printer_test.TestRecord {:bar \\y, :foo \\x}"
             (pprint-str r {:width 200})))))
  (testing "vars"
    (let [v #'*options*]
      (is (= "#'puget.printer/*options*"
             (pprint-str v)))))
  (testing "functions"
    (is (re-seq #"#<Fn@[0-9a-z]+ puget\.printer/pretty_printer>"
                (pprint-str pretty-printer)))
    (is (re-seq #"#<Fn@[0-9a-z]+ puget\.printer/tagged_handler\[handler\]"
                (pprint-str (tagged-handler 'foo str)))))
  (testing "atom"
    (let [v (atom :foo)]
      (is (re-seq #"#<Atom@[0-9a-f]+ :foo>" (pprint-str v)))))
  (testing "delay"
    (let [v (delay (+ 8 14))]
      (is (re-seq #"#<Delay@[0-9a-f]+ pending>" (pprint-str v)))
      (is (= 22 @v))
      (is (re-seq #"#<Delay@[0-9a-f]+ 22>" (pprint-str v)))))
  (testing "future"
    (let [v (future (do (Thread/sleep 100) :done))]
      (is (re-seq #"#<Future@[0-9a-f]+ pending>" (pprint-str v)))
      (is (= :done @v))
      (is (re-seq #"#<Future@[0-9a-f]+ :done>" (pprint-str v)))))
  (testing "custom IPending, realized"
    (let [v (->APending true)]
      (is (re-seq #"#<puget\.printer_test\.APending@[0-9a-f]+ 1"
                  (pprint-str v)))))
  (testing "custom IPending, not realized"
    (let [v (->APending false)]
      (is (re-seq #"#<puget\.printer_test\.APending@[0-9a-f]+ pending"
                  (pprint-str v))))))


(deftest pretty-metadata
  (testing "print-meta logic"
    (let [value ^:foo [:bar]]
      (binding [*print-meta* true]
        (is (= "^{:foo true}\n[:bar]" (pprint-str value)))
        (is (= "[:bar]" (pprint-str value {:print-meta false}))))
      (binding [*print-meta* false]
        (is (= "^{:foo true}\n[:bar]" (pprint-str value {:print-meta true})))))))


(deftest pretty-collection-options
  (testing "collection key sorting"
    (let [set1 (set [:zeta :book])
          map1 (array-map :b 0 :a 1)
          map2 (array-map :z 2 :a 5 :m 8)]
      (testing "never sort"
        (with-options {:sort-keys false}
          (is (= "#{:zeta :book}" (pprint-str set1)))
          (is (= "{:b 0, :a 1}" (pprint-str map1)))))
      (testing "sort at counted threshold"
        (with-options {:sort-keys 2}
          (is (= "{:a 1, :b 0}" (pprint-str map1))
              "collection smaller than threshold should be sorted")
          (is (= "{:z 2, :a 5, :m 8}" (pprint-str map2))
              "collection larger than threshold should not be sorted")))
      (testing "always sort"
        (with-options {:sort-keys true}
          (is (= "{:a 1, :b 0}" (pprint-str map1)))
          (is (= "{:a 5, :m 8, :z 2}" (pprint-str map2)))))
      (testing "sorted colls"
        (with-options {:sort-keys true}
          (is (= "#{3 2 1}" (pprint-str (sorted-set-by > 1 2 3)))
              "sorted collection should not be reordered")))))
  (testing "map delimiter"
    (is (= "{:a 0, :b 1}" (pprint-str {:a 0, :b 1}))
        "default separator is a comma")
    (with-options {:map-delimiter " <==>"}
      (is (= "{:a 0 <==> :b 1}" (pprint-str {:a 0, :b 1})))))
  (testing "map collection separator"
    (with-options {:map-coll-separator :line, :width 10}
      (is (= "{:bar\n [:a :b]}" (pprint-str {:bar [:a :b]})))))
  (testing "namespace maps"
    (with-options {:namespace-maps true}
      (is (= "{:b 3, :a/x 1, :a/y 2}" (pprint-str {:a/x 1, :a/y 2, :b 3}))
          "any simple keys should prevent namespacing")
      (is (= "#:a {:x 1, :y 2}" (pprint-str {:a/x 1, :a/y 2}))
          "map with all common qualified keys should be namespaced")
      (is (= "{:a/x 1, :b/x 2}" (pprint-str {:a/x 1, :b/x 2}))
          "map with insufficiently common qualifiers should not be namespaced")
      (is (= "#:a {:x 1, :y 2, :b/x 3}" (pprint-str {:a/x 1, :a/y 2, :b/x 3}))
          "common ns should be qualified even with other ns keys")
      (is (= "{\"a/x\" 1, :a/y 2}" (pprint-str {"a/x" 1, :a/y 2}))
          "any non-ident keys should prevent namespacing")))
  (testing "lazy seq limits"
    (with-options {:seq-limit 4}
      (is (= "(1 2 3)" (pprint-str (map inc [0 1 2]))))
      (is (= "(0 1 2 3 ...)" (pprint-str (range 100)))))))


(deftest pretty-color-options
  (let [value [nil 1.0 true "foo" :bar]
        bw-str (with-out-str (pprint value))
        colored-str (with-out-str (cprint value))
        thin-str (cprint-str value {:width 5})]
    (is (> (count colored-str) (count bw-str)))
    (is (not= colored-str thin-str))
    (is (= "123" (with-color (color-text :frobble "123"))))
    (is (= "#{:baz}" (pprint-str #{:baz})))
    (is (= (cprint-str :foo)
           (with-color (color-text :keyword ":foo"))))))


(deftest pretty-extensions
  (let [cv (ComplexValue.)]
    (testing "custom print handler"
      (with-options {:print-handlers {ComplexValue (tagged-handler 'complex/val str)}}
        (is (= "#complex/val \"to-string\"" (pprint-str cv)))))
    (testing "standard pr-handler"
      (with-options {:print-handlers {ComplexValue pr-handler}
                     :print-fallback :error}
        (is (= "{{ complex value print }}" (pprint-str cv)))))
    (testing "standard unknown-handler"
      (with-options {:print-handlers {ComplexValue unknown-handler}
                     :print-fallback :pretty}
        (is (re-seq #"#<puget\.printer_test\.ComplexValue@[0-9a-f]+ to-string>"
                    (pprint-str cv)))))
    (testing "standard print fallback"
      (with-options {:print-fallback :pretty}
        (is (re-seq #"#<puget\.printer_test\.ComplexValue@[0-9a-f]+ to-string>"
                    (pprint-str cv)))))
    (testing "built-in print fallback"
      (with-options {:print-fallback :print}
        (is (= "{{ complex value print }}" (pprint-str cv)))))
    (testing "error print fallback"
      (with-options {:print-fallback :error}
        (is (thrown? IllegalArgumentException
              (pprint-str cv)))))
    (testing "handler function print-fallback"
      (with-options {:print-fallback (constantly [:span "custom-fn"])}
        (is (= "custom-fn" (pprint-str cv)))))
    (testing "illegal print fallback"
      (with-options {:print-fallback "some other type"}
        (is (thrown? IllegalStateException (pprint-str cv)))))))
