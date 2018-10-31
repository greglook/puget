(ns puget.dispatch-test
  (:require
    [clojure.test :refer :all]
    [puget.dispatch :as dispatch]))


(deftest symbolic-dispatch
  (let [dispatch (dispatch/symbolic-lookup
                   {'java.lang.String :string
                    'java.io.FileInputStream :file-input-stream})]
    (is (= :string (dispatch java.lang.String)))
    (is (= :file-input-stream (dispatch java.io.FileInputStream)))
    (is (nil? (dispatch java.lang.Boolean)))))


(deftest chained-dispatch
  (testing "Empty chained dispatch"
    (is (thrown? IllegalArgumentException
          (dispatch/chained-lookup [nil nil]))))
  (testing "Single chained dispatch"
    (let [dispatch (dispatch/chained-lookup [{:foo :bar}])]
      (is (= :bar (dispatch :foo)))
      (is (nil? (dispatch :baz)))))
  (testing "Multiple chained dispatch"
    (let [dispatch (dispatch/chained-lookup
                     {:foo :bar}
                     {:baz :qux})]
      (is (= :bar (dispatch :foo)))
      (is (= :qux (dispatch :baz)))
      (is (nil? (dispatch :abc))))))


(deftest caching-dispatch
  (let [calls (atom [])
        dispatch (dispatch/caching-lookup
                   (fn [t]
                     (swap! calls conj t)
                     (get {:foo :bar, :bar false, :baz nil} t)))]
    (testing "caches truthy values"
      (is (= :bar (dispatch :foo)))
      (is (= [:foo] @calls))
      (is (= :bar (dispatch :foo)))
      (is (= [:foo] @calls)))
    (testing "caches false values"
      (is (false? (dispatch :bar)))
      (is (= [:foo :bar] @calls))
      (is (false? (dispatch :bar)))
      (is (= [:foo :bar] @calls)))
    (testing "caches nil values"
      (is (nil? (dispatch :baz)))
      (is (= [:foo :bar :baz] @calls))
      (is (nil? (dispatch :baz)))
      (is (= [:foo :bar :baz] @calls)))))


(defn printing-lookup
  [dispatch]
  (fn lookup
    [t]
    (let [v (dispatch t)]
      (println t "=>" v)
      v)))


(deftest inheritance-dispatch
  (testing "direct class lookup"
    (let [dispatch (dispatch/inheritance-lookup
                     {java.util.concurrent.Future :future
                      clojure.lang.IPending :pending})]
      (is (= :future (dispatch java.util.concurrent.Future))
          "Direct class lookup should return value")))
  (testing "ancestor class lookup"
    (let [dispatch (dispatch/inheritance-lookup
                     {clojure.lang.APersistentMap :abstract-persistent-map
                      clojure.lang.AFn :abstract-fn
                      clojure.lang.ILookup :interface-lookup})]
      (is (= :abstract-persistent-map (dispatch clojure.lang.PersistentArrayMap)))))
  (testing "interface lookup"
    (let [dispatch (dispatch/inheritance-lookup
                     {clojure.lang.ILookup :interface-lookup
                      java.io.InputStream :input-stream})]
      (is (= :interface-lookup (dispatch clojure.lang.PersistentHashMap)))
      (is (nil? (dispatch clojure.lang.Keyword))))
    (let [dispatch (dispatch/inheritance-lookup
                     {clojure.lang.ILookup :interface-lookup
                      java.util.Map :java-map})]
      (is (thrown? RuntimeException
            (dispatch clojure.lang.PersistentHashMap))))))
