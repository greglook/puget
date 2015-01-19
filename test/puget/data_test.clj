(ns puget.data-test
  (:require
    [clojure.edn :as edn]
    [clojure.test :refer :all]
    [puget.data :as data]))


(defn test-tagged-value
  [data t v & [reader]]
  (let [{:keys [tag value]} (data/->edn data)]
    (is (= t tag))
    (is (= v value)))
  (let [s (data/edn-str data)
        data' (edn/read-string (if reader {:readers {t reader}} {}) s)]
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
        {:keys [tag value]} (data/->edn byte-arr)
        string (data/edn-str byte-arr)
        read-arr (edn/read-string {:readers {'bin data/read-bin}} string)]
    (is (= 'bin tag))
    (is (= "Zm9vYmFyYmF6" value))
    (is (= (count byte-arr) (count read-arr)))
    (is (= (seq byte-arr) (seq read-arr)))))


(deftest generic-tagged-value
  (let [data (data/tagged-value 'foo :bar)
        {:keys [tag value]} (data/->edn data)
        string (data/edn-str data)]
    (is (= 'foo tag))
    (is (= :bar value))
    (is (= "#foo :bar" (str data)))
    (is (= data (edn/read-string {:default data/tagged-value} string)))))


(defrecord TestRecord [x y])
(data/extend-tagged-map TestRecord 'test/record)
(deftest tagged-value-extension
  (let [rec (TestRecord. :foo :bar)
        {:keys [tag value]} (data/->edn rec)]
    (is (= 'test/record tag))
    (is (= {:x :foo, :y :bar} value))))
