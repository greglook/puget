(ns puget.data-test
  (:require
    [clojure.test :refer :all]
    [puget.data :refer :all]))


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
    (is (= (sort total-order elements)
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


(deftest built-in-tagged-values
  (testing "TaggedValue"
    (are [data t v] (and (= t (edn-tag data)) (= v (edn-value data)))
         (byte-array 10)
         'bin "AAAAAAAAAAAAAA=="

         (java.util.Date. 1383271402749)
         'inst "2013-11-01T02:03:22.749-00:00"

         (java.util.UUID/fromString "96d91316-53b9-4800-81c1-97ae9f4b86b0")
         'uuid "96d91316-53b9-4800-81c1-97ae9f4b86b0"

         (java.net.URI. "http://en.wikipedia.org/wiki/Uniform_resource_identifier")
         'uri "http://en.wikipedia.org/wiki/Uniform_resource_identifier")))


(deftest bin-reading
  (let [byte-arr (.getBytes "foobarbaz")
        value-str (edn-value byte-arr)
        read-arr (read-bin value-str)]
    (is (= (count byte-arr) (count read-arr)))
    (is (= (seq byte-arr) (seq read-arr)))))


(deftest uri-reading
  (let [uri (java.net.URI. "urn:isbn:0-486-27557-4")]
    (is (= uri (read-uri (edn-value uri))))))
