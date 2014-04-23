(ns puget.data-test
  (:require
    [clojure.edn :as edn]
    [clojure.test :refer :all]
    [puget.data :as data]))


(deftest total-ordering
  (let [t (java.util.Date. 1234567890)
        elements [-123 4096N 3.14159M 0.0
                  nil true false
                  \c \b \a
                  "thirteen" "one"
                  :foo :my-ns/bar :a-ns/baz :zap
                  'x 'aaa/foo 'z/bar 'y
                  t
                  {:x 1 :y 2}
                  '(1 2 3 4)
                  [1 2 3]
                  #{'howdy 'doody}
                  '(2 3 4)
                  'x]]
    (is (= (sort data/total-order elements)
           [nil false true -123 0.0 3.14159M 4096N
            \a \b \c "one" "thirteen"
            :foo :zap :a-ns/baz :my-ns/bar
            'x 'x 'y 'aaa/foo 'z/bar
            '(1 2 3 4)
            '(2 3 4)
            [1 2 3]
            #{'howdy 'doody}
            {:x 1 :y 2}
            t]))))


(defrecord TestRecord [x y])
(data/extend-tagged-map TestRecord 'test/record)
(deftest tagged-value-extension
  (let [rec (TestRecord. :foo :bar)]
    (is (= 'test/record (data/edn-tag rec)))
    (is (= {:x :foo, :y :bar} (data/edn-value rec)))))


(deftest generic-tagged-value
  (let [data (data/tagged-value 'foo :bar)
        string (data/edn-str data)]
    (is (= 'foo (data/edn-tag data)))
    (is (= :bar (data/edn-value data)))
    (is (= data (edn/read-string {:default data/tagged-value} string)))))


(deftest byte-arrays
  (let [byte-arr (.getBytes "foobarbaz")
        string (data/edn-str byte-arr)
        read-arr (edn/read-string {:readers {'bin data/read-bin}} string)]
    (is (= 'bin (data/edn-tag byte-arr)))
    (is (= "Zm9vYmFyYmF6" (data/edn-value byte-arr)))
    (is (= (count byte-arr) (count read-arr)))
    (is (= (seq byte-arr) (seq read-arr)))))


(defn test-tagged-value
  [data t v]
  (is (= t (data/edn-tag data)))
  (is (= v (data/edn-value data)))
  (let [s (data/edn-str data)]
    (is (= s (pr-str data)))))


(deftest built-in-printing
  (testing "TaggedValue"
    (test-tagged-value
      (java.util.Date. 1383271402749)
      'inst "2013-11-01T02:03:22.749-00:00")
    (test-tagged-value
      (java.util.UUID/fromString "96d91316-53b9-4800-81c1-97ae9f4b86b0")
     'uuid "96d91316-53b9-4800-81c1-97ae9f4b86b0")
    (test-tagged-value
     (java.net.URI. "http://en.wikipedia.org/wiki/Uniform_resource_identifier")
     'uri "http://en.wikipedia.org/wiki/Uniform_resource_identifier")))
