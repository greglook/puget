(ns puget.data-test
  (:require
    [clojure.edn :as edn]
    [clojure.test :refer :all]
    [puget.data :as data]))


(defn test-tagged-literal
  [data t f]
  (let [{:keys [tag form]} (data/->edn data)]
    (is (= t tag))
    (is (= f form))))


(deftest inst-tagged-literals
  (test-tagged-literal
    (java.util.Date. 1383271402749)
    'inst "2013-11-01T02:03:22.749-00:00"))


(deftest uuid-tagged-literals
  (test-tagged-literal
    (java.util.UUID/fromString "96d91316-53b9-4800-81c1-97ae9f4b86b0")
    'uuid "96d91316-53b9-4800-81c1-97ae9f4b86b0"))


(defrecord TestRecord [x y])
(data/extend-tagged-map TestRecord 'test/record)
(deftest tagged-literal-extension
  (let [rec (TestRecord. :foo :bar)
        {:keys [tag form]} (data/->edn rec)]
    (is (= 'test/record tag))
    (is (= {:x :foo, :y :bar} form))))
