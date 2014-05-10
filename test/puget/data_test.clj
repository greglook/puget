(ns puget.data-test
  (:require
    [clojure.edn :as edn]
    [clojure.test :refer :all]
    [puget.data :as data]))


(defn- is-sorted
  [& values]
  (is (= values (sort data/total-order (shuffle values)))))


(deftest order-primitives
  (is-sorted
    nil false true 0 \a "a" :a 'a))

(deftest order-numbers
  (is-sorted
    -123 0.0 3.14159M 4096N))

(deftest order-strings
  (is-sorted
    "alpha" "alphabet" "beta" "omega"))

(deftest order-keywords
  (is-sorted
    :foo :zap :a-ns/baz :my-ns/bar))

(deftest order-symbols
  (is-sorted
    'x 'y 'aaa/foo 'z/bar))

(deftest order-sequences
  (is-sorted
    '(1 2 3) [1 2 3] [1 2 3 4] [1 2 4] [1 \2 "3"] [\1] #{\1}))

(deftest order-sets
  (is-sorted
    #{:one} #{:two} #{:zzz} #{:one :two} #{:one :zzz}))

(deftest order-maps
  (is-sorted
    {:a 1 :b 2/3} {:a 1 :b 2/3 :c 'x} {:a 1 :b 4/3} {:x 1 :y 2}))

(deftest order-classes
  (is-sorted
    (java.util.Currency/getInstance "JPY")
    (java.util.Currency/getInstance "USD")
    (java.util.Date. 1234567890)
    (java.util.Date. 1234567891)))


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
