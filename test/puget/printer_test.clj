(ns puget.printer-test
  (:require
    [clojure.test :refer [deftest testing are is]]
    [puget.printer :as p]))


;; ## Test Types

(defrecord TestRecord
  [foo bar])


(deftype APending
  [is-realized]

  clojure.lang.IDeref

  (deref [_] 1)


  clojure.lang.IPending

  (isRealized [_] is-realized))


(deftype ComplexValue
  []

  Object

  (toString [_] "to-string"))


(defmethod print-method ComplexValue
  [_ w]
  (.write w "{{ complex value print }}"))


;; ## Canonical Printing

(deftest canonical-primitives
  (let [printer (p/canonical-printer)]
    (are [v text] (= text (p/render-str printer v))
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
  (let [printer (p/canonical-printer)]
    (are [v text] (= text (p/render-str printer v))
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
  (let [printer (p/canonical-printer)
        value ^:foo [:bar]]
    (binding [*print-meta* true]
      (is (= "[:bar]" (p/render-str printer value))
          "should not render metadata"))))


(deftest canonical-extensions
  (testing "tagged-handler construction"
    (is (thrown? clojure.lang.ExceptionInfo (p/tagged-handler "foo" str)))
    (is (thrown? clojure.lang.ExceptionInfo (p/tagged-handler 'foo "abcd")))
    (is (ifn? (p/tagged-handler 'foo str))))
  (let [handlers {java.util.UUID (p/tagged-handler 'uuid str)}
        printer (p/canonical-printer handlers)
        uuid-str "31f7dd72-c7f7-4a15-a98b-0f9248d3aaa6"]
    (is (= (str "#uuid \"" uuid-str "\"")
           (p/render-str printer (java.util.UUID/fromString uuid-str))))))


(deftest canonical-errors
  (let [printer (p/canonical-printer)]
    (are [v] (thrown? IllegalArgumentException
               (p/render-str printer v))
      #"^foo"
      #'p/*options*
      (delay 5)
      (atom :foo)
      (->TestRecord :x \y)
      (java.util.Currency/getInstance "USD"))))


;; ## Pretty Printing

(deftest pretty-primitives
  (are [v text] (= text (p/pprint-str v))
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
  (are [v text] (= text (p/pprint-str v))
    '()                    "()"
    '(foo :bar)            "(foo :bar)"
    '(1 2 3)               "(1 2 3)"
    []                     "[]"
    [4 "five" 6.0]         "[4 \"five\" 6.0]"
    {}                     "{}"
    {:foo 8 :bar 'baz}     "{:bar baz, :foo 8}"     ; gets sorted
    #{}                    "#{}"
    #{:omega :alpha :beta} "#{:alpha :beta :omega}" ; also sorted
    (lazy-seq [:x])        "(:x)"
    (first {:a 1})         "[:a 1]"))


(deftest pretty-java-types
  (testing "class types"
    (is (re-seq #"#<Class@[0-9a-f]+ java\.util\.Date>"
                (p/pprint-str java.util.Date))))
  (testing "regex"
    (let [v #"\d+"]
      (is (= "#\"\\d+\"" (p/pprint-str v)))))
  (testing "date instants"
    (is (= "#inst \"2015-10-12T05:23:08.000-00:00\""
           (p/pprint-str (java.util.Date. 1444627388000)))))
  (testing "UUIDs"
    (let [uuid-str "31f7dd72-c7f7-4a15-a98b-0f9248d3aaa6"]
      (is (= (str "#uuid \"" uuid-str "\"")
             (p/pprint-str (java.util.UUID/fromString uuid-str)))))))


(deftest pretty-clojure-types
  (testing "records"
    (let [r (->TestRecord \x \y)]
      (is (= "#puget.printer_test.TestRecord\n{:bar \\y, :foo \\x}"
             (p/pprint-str r {:width 30}))
          "long record prints with form on new line")
      (is (= "#puget.printer_test.TestRecord {:bar \\y, :foo \\x}"
             (p/pprint-str r {:width 200})))))
  (testing "vars"
    (let [v #'p/*options*]
      (is (= "#'puget.printer/*options*"
             (p/pprint-str v)))))
  (testing "functions"
    (is (re-seq #"#<Fn@[0-9a-z]+ puget\.printer/pretty_printer>"
                (p/pprint-str p/pretty-printer)))
    (is (re-seq #"#<Fn@[0-9a-z]+ puget\.printer/tagged_handler\[handler\]"
                (p/pprint-str (p/tagged-handler 'foo str)))))
  (testing "atom"
    (let [v (atom :foo)]
      (is (re-seq #"#<Atom@[0-9a-f]+ :foo>" (p/pprint-str v)))))
  (testing "delay"
    (let [v (delay (+ 8 14))]
      (is (re-seq #"#<Delay@[0-9a-f]+ pending>" (p/pprint-str v)))
      (is (= 22 @v))
      (is (re-seq #"#<Delay@[0-9a-f]+ 22>" (p/pprint-str v)))))
  (testing "future"
    (let [v (future (Thread/sleep 100) :done)]
      (is (re-seq #"#<Future@[0-9a-f]+ pending>" (p/pprint-str v)))
      (is (= :done @v))
      (is (re-seq #"#<Future@[0-9a-f]+ :done>" (p/pprint-str v)))))
  (testing "custom IPending, realized"
    (let [v (->APending true)]
      (is (re-seq #"#<puget\.printer_test\.APending@[0-9a-f]+ 1"
                  (p/pprint-str v)))))
  (testing "custom IPending, not realized"
    (let [v (->APending false)]
      (is (re-seq #"#<puget\.printer_test\.APending@[0-9a-f]+ pending"
                  (p/pprint-str v))))))


(deftest pretty-metadata
  (testing "print-meta logic"
    (let [value ^:foo [:bar]]
      (binding [*print-meta* true]
        (is (= "^{:foo true}\n[:bar]" (p/pprint-str value)))
        (is (= "[:bar]" (p/pprint-str value {:print-meta false}))))
      (binding [*print-meta* false]
        (is (= "^{:foo true}\n[:bar]" (p/pprint-str value {:print-meta true})))))))


(deftest pretty-collection-options
  (testing "collection key sorting"
    (let [set1 (set [:zeta :book])
          map1 (array-map :b 0 :a 1)
          map2 (array-map :z 2 :a 5 :m 8)]
      (testing "never sort"
        (p/with-options {:sort-keys false}
          (is (= "#{:zeta :book}" (p/pprint-str set1)))
          (is (= "{:b 0, :a 1}" (p/pprint-str map1)))))
      (testing "sort at counted threshold"
        (p/with-options {:sort-keys 2}
          (is (= "{:a 1, :b 0}" (p/pprint-str map1))
              "collection smaller than threshold should be sorted")
          (is (= "{:z 2, :a 5, :m 8}" (p/pprint-str map2))
              "collection larger than threshold should not be sorted")))
      (testing "always sort"
        (p/with-options {:sort-keys true}
          (is (= "{:a 1, :b 0}" (p/pprint-str map1)))
          (is (= "{:a 5, :m 8, :z 2}" (p/pprint-str map2)))))
      (testing "sorted colls"
        (p/with-options {:sort-keys true}
          (is (= "#{3 2 1}" (p/pprint-str (sorted-set-by > 1 2 3)))
              "sorted collection should not be reordered")))))
  (testing "map delimiter"
    (is (= "{:a 0, :b 1}" (p/pprint-str {:a 0, :b 1}))
        "default separator is a comma")
    (p/with-options {:map-delimiter " <==>"}
      (is (= "{:a 0 <==> :b 1}" (p/pprint-str {:a 0, :b 1})))))
  (testing "map collection separator"
    (p/with-options {:map-coll-separator :line, :width 10}
      (is (= "{:bar\n [:a :b]}" (p/pprint-str {:bar [:a :b]})))))
  (testing "namespace maps"
    (p/with-options {:namespace-maps true}
      (is (= "{:b 3, :a/x 1, :a/y 2}" (p/pprint-str {:a/x 1, :a/y 2, :b 3}))
          "any simple keys should prevent namespacing")
      (is (= "#:a {:x 1, :y 2}" (p/pprint-str {:a/x 1, :a/y 2}))
          "map with all common qualified keys should be namespaced")
      (is (= "{:a/x 1, :b/x 2}" (p/pprint-str {:a/x 1, :b/x 2}))
          "map with insufficiently common qualifiers should not be namespaced")
      (is (= "#:a {:x 1, :y 2, :b/x 3}" (p/pprint-str {:a/x 1, :a/y 2, :b/x 3}))
          "common ns should be qualified even with other ns keys")
      (is (= "{\"a/x\" 1, :a/y 2}" (p/pprint-str {"a/x" 1, :a/y 2}))
          "any non-ident keys should prevent namespacing")))
  (testing "lazy - seq-limit only"
    (p/with-options {:seq-limit 4 :coll-limit nil}
      (is (= "(1 2 3)" (p/pprint-str (map inc [0 1 2]))))
      (is (= "(6 7 8 9)" (p/pprint-str (range 6 10))))
      (is (= "(0 1 2 3 ...)" (p/pprint-str (range 100))))))
  (testing "lazy - coll-limit only"
    (p/with-options {:seq-limit nil :coll-limit 4}
      (is (= "(1 2 3)" (p/pprint-str (map inc [0 1 2]))))
      (is (= "(0 1 2 3 ...)" (p/pprint-str (range 100))))))
  (testing "lazy - seq-limit has higher precesence than coll-limit"
    (p/with-options {:seq-limit 5 :coll-limit 4}
      (is (= "(1 2 3)" (p/pprint-str (map inc [0 1 2]))))
      (is (= "(0 1 2 3 4 ...)" (p/pprint-str (range 100))))))
  (testing "coll-limit on lists"
    (p/with-options {:coll-limit 4}
      (is (= "(0 1 2)" (p/pprint-str '(0 1 2))))
      (is (= "(0 1 2 3 ...)" (p/pprint-str '(0 1 2 3 4 5))))))
  (testing "coll-limit on vectors"
    (p/with-options {:coll-limit 4}
      (is (= "[0 1 2]" (p/pprint-str [0 1 2])))
      (is (= "[5 4 3 2 ...]" (p/pprint-str [5 4 3 2 1])))))
  (testing "coll-limit on sets"
    (p/with-options {:coll-limit 4}
      (is (= "#{1 2 3}" (p/pprint-str #{3 2 1})))
      (is (= "#{1 4 3 2 ...}" (p/pprint-str #{5 4 3 2 1})))))
  (testing "coll-limit on maps"
    (p/with-options {:coll-limit 3}
      (is (= "{:a 1, :b 2, :c 3}" (p/pprint-str {:a 1 :b 2 :c 3})))
      (is (= "{:a 1, :b 2, :c 3}" (p/pprint-str {:c 3 :b 2 :a 1})))
      (is (= "{:a 1, :b 2, :c 3, ...}" (p/pprint-str {:a 1 :b 2 :c 3 :d 4})))
      (is (= "{:d 4, :c 3, :b 2, ...}" (p/pprint-str {:d 4 :c 3 :b 2 :a 1}))))))


(deftest pretty-color-options
  (let [value [nil 1.0 true "foo" :bar]
        bw-str (with-out-str (p/pprint value))
        colored-str (with-out-str (p/cprint value))
        thin-str (p/cprint-str value {:width 5})]
    (is (> (count colored-str) (count bw-str)))
    (is (not= colored-str thin-str))
    (is (= "123" (p/with-color (p/color-text :frobble "123"))))
    (is (= "#{:baz}" (p/pprint-str #{:baz})))
    (is (= (p/cprint-str :foo)
           (p/with-color (p/color-text :keyword ":foo"))))))


(deftest pretty-extensions
  (let [cv (ComplexValue.)]
    (testing "custom print handler"
      (p/with-options {:print-handlers {ComplexValue (p/tagged-handler 'complex/val str)}}
        (is (= "#complex/val \"to-string\"" (p/pprint-str cv)))))
    (testing "standard pr-handler"
      (p/with-options {:print-handlers {ComplexValue p/pr-handler}
                       :print-fallback :error}
        (is (= "{{ complex value print }}" (p/pprint-str cv)))))
    (testing "standard unknown-handler"
      (p/with-options {:print-handlers {ComplexValue p/unknown-handler}
                       :print-fallback :pretty}
        (is (re-seq #"#<puget\.printer_test\.ComplexValue@[0-9a-f]+ to-string>"
                    (p/pprint-str cv)))))
    (testing "standard print fallback"
      (p/with-options {:print-fallback :pretty}
        (is (re-seq #"#<puget\.printer_test\.ComplexValue@[0-9a-f]+ to-string>"
                    (p/pprint-str cv)))))
    (testing "built-in print fallback"
      (p/with-options {:print-fallback :print}
        (is (= "{{ complex value print }}" (p/pprint-str cv)))))
    (testing "error print fallback"
      (p/with-options {:print-fallback :error}
        (is (thrown? IllegalArgumentException
              (p/pprint-str cv)))))
    (testing "handler function print-fallback"
      (p/with-options {:print-fallback (constantly [:span "custom-fn"])}
        (is (= "custom-fn" (p/pprint-str cv)))))
    (testing "illegal print fallback"
      (p/with-options {:print-fallback "some other type"}
        (is (thrown? IllegalStateException (p/pprint-str cv)))))))
