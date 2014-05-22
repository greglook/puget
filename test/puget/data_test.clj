(ns puget.data-test
  (:require
    [clojure.edn :as edn]
    [clojure.test :refer :all]
    [puget.data :as data]))


(defn test-tagged-value
  [data t v & [reader]]
  (is (= t (data/edn-tag data)))
  (is (= v (data/edn-value data)))
  (let [s (data/edn-str data)
        data' (edn/read-string (if reader {:readers {t reader}} {}) s)]
    (is (= s (pr-str data)))
    (is (= data data'))))


(deftest inst-tagged-values
  (test-tagged-value
    (java.util.Date. 1383271402749)
    'inst "2013-11-01T02:03:22.749-00:00"))


(deftest uuid-tagged-values
  (test-tagged-value
    (java.util.UUID/fromString "96d91316-53b9-4800-81c1-97ae9f4b86b0")
    'uuid "96d91316-53b9-4800-81c1-97ae9f4b86b0"))


(deftest uri-tagged-values
  (test-tagged-value
    (java.net.URI. "http://en.wikipedia.org/wiki/Uniform_resource_identifier")
    'uri "http://en.wikipedia.org/wiki/Uniform_resource_identifier"
    data/read-uri))


(deftest bin-tagged-values
  (let [byte-arr (.getBytes "foobarbaz")
        string (data/edn-str byte-arr)
        read-arr (edn/read-string {:readers {'bin data/read-bin}} string)]
    (is (= 'bin (data/edn-tag byte-arr)))
    (is (= "Zm9vYmFyYmF6" (data/edn-value byte-arr)))
    (is (= (count byte-arr) (count read-arr)))
    (is (= (seq byte-arr) (seq read-arr)))))


(deftest generic-tagged-value
  (let [data (data/tagged-value 'foo :bar)
        string (data/edn-str data)]
    (is (= 'foo (data/edn-tag data)))
    (is (= :bar (data/edn-value data)))
    (is (= "#foo :bar" (pr-str data)))
    (is (= data (edn/read-string {:default data/tagged-value} string)))))


(defrecord TestRecord [x y])
(data/extend-tagged-map TestRecord 'test/record)
(deftest tagged-value-extension
  (let [rec (TestRecord. :foo :bar)]
    (is (= 'test/record (data/edn-tag rec)))
    (is (= {:x :foo, :y :bar} (data/edn-value rec)))))
