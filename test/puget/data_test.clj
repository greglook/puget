(ns puget.data-test
  (:require
    [clojure.edn :as edn]
    [clojure.test :refer :all]
    [puget.data :as data]))


(defn test-tagged-literal
  [data t f & [reader]]
  (let [{:keys [tag form]} (data/->edn data)]
    (is (= t tag))
    (is (= f form)))
  (let [s (data/edn-str data)
        data' (edn/read-string (if reader {:readers {t reader}} {}) s)]
    (is (= data data'))))


(deftest inst-tagged-literals
  (test-tagged-literal
    (java.util.Date. 1383271402749)
    'inst "2013-11-01T02:03:22.749-00:00"))


(deftest uuid-tagged-literals
  (test-tagged-literal
    (java.util.UUID/fromString "96d91316-53b9-4800-81c1-97ae9f4b86b0")
    'uuid "96d91316-53b9-4800-81c1-97ae9f4b86b0"))


(deftest uri-tagged-literals
  (test-tagged-literal
    (java.net.URI. "http://en.wikipedia.org/wiki/Uniform_resource_identifier")
    'puget/uri "http://en.wikipedia.org/wiki/Uniform_resource_identifier"
    data/read-uri))


(deftest bin-tagged-literals
  (let [byte-arr (.getBytes "foobarbaz")
        {:keys [tag form]} (data/->edn byte-arr)
        string (data/edn-str byte-arr)
        read-arr (edn/read-string {:readers {'puget/bin data/read-bin}} string)]
    (is (= 'puget/bin tag))
    (is (= "Zm9vYmFyYmF6" form))
    (is (= (count byte-arr) (count read-arr)))
    (is (= (seq byte-arr) (seq read-arr)))))


(deftest generic-tagged-literal
  (let [data (data/tagged-literal 'foo :bar)
        {:keys [tag form]} (data/->edn data)
        string (data/edn-str data)]
    (is (data/tagged-literal? data))
    (is (= 'foo tag))
    (is (= :bar form))
    (is (= "#foo :bar" string (str data)))
    (is (= data (edn/read-string {:default data/tagged-literal} string)))))


(defrecord TestRecord [x y])
(data/extend-tagged-map TestRecord 'test/record)
(deftest tagged-literal-extension
  (let [rec (TestRecord. :foo :bar)
        {:keys [tag form]} (data/->edn rec)]
    (is (= 'test/record tag))
    (is (= {:x :foo, :y :bar} form))))
