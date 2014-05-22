(ns puget.order-test
  (:require
    [clojure.test :refer :all]
    [puget.order :as order]))


(defn- is-sorted
  [& values]
  (dotimes [_ 10]
    (is (= values (sort order/rank (shuffle values))))))


(deftest primitive-ordering
  (is-sorted
    nil false true 0 \a "a" :a 'a))


(deftest number-ordering
  (is-sorted
    -123 0.0 3.14159M 4096N))


(deftest string-ordering
  (is-sorted
    "alpha" "alphabet" "beta" "omega"))


(deftest keyword-ordering
  (is-sorted
    :foo :zap :a-ns/baz :my-ns/bar))


(deftest symbol-ordering
  (is-sorted
    'x 'y 'aaa/foo 'z/bar))


(deftest sequence-ordering
  (is-sorted
    '(1 2 3) [1 2 3] [1 2 3 4] [1 2 4] [1 \2 "3"] [\1] #{\1}))


(deftest set-ordering
  (is-sorted
    #{:one} #{:two} #{:zzz} #{:one :two} #{:one :zzz}))


(deftest map-ordering
  (is-sorted
    {:a 1 :b 2/3} {:a 1 :b 2/3 :c 'x} {:a 1 :b 4/3} {:x 1 :y 2}))


(deftest class-ordering
  (is-sorted
    (java.util.Currency/getInstance "JPY")
    (java.util.Currency/getInstance "USD")
    (java.util.Date. 1234567890)
    (java.util.Date. 1234567891)))
