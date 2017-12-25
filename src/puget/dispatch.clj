(ns puget.dispatch
  "Dispatch functions take a `Class` argument and return the looked-up value.
  This provides similar functionality to Clojure's protocols, but operates over
  locally-constructed logic rather than using a global dispatch table.

  A simple example is a map from classes to values, which can be used directly
  as a lookup function."
  (:require
    [clojure.string :as str]))


;; ## Logical Dispatch

(defn chained-lookup
  "Builds a dispatcher which looks up a type by checking multiple dispatchers
  in order until a matching entry is found. Takes either a single collection of
  dispatchers or a variable list of dispatcher arguments. Ignores nil
  dispatchers in the sequence."
  ([dispatchers]
   {:pre [(sequential? dispatchers)]}
   (let [candidates (remove nil? dispatchers)]
     (when (empty? candidates)
       (throw (IllegalArgumentException.
                "chained-lookup must be provided at least one dispatch function to try.")))
     (if (= 1 (count candidates))
       (first candidates)
       (fn lookup
         [t]
         (some #(% t) candidates)))))
  ([a b & more]
   (chained-lookup (list* a b more))))


(defn caching-lookup
  "Builds a dispatcher which caches values returned for each type. This improves
  performance when the underlying dispatcher may need to perform complex
  lookup logic to determine the dispatched value."
  [dispatch]
  (let [cache (atom {})]
    (fn lookup
      [t]
      (let [memory @cache]
        (if (contains? memory t)
          (get memory t)
          (let [v (dispatch t)]
            (swap! cache assoc t v)
            v))))))


;; ## Type Dispatch

(defn symbolic-lookup
  "Builds a dispatcher which looks up a type by checking the underlying lookup
  using the type's _symbolic_ name, rather than the class value itself. This is
  useful for checking configuration that must be created in situations where the
  classes themselves may not be loaded yet."
  [dispatch]
  (fn lookup
    [^Class t]
    (dispatch (symbol (.getName t)))))


(defn- lineage
  "Returns the ancestry of the given class, starting with the class and
  excluding the `java.lang.Object` base class."
  [cls]
  (take-while #(and (some? %) (not= Object %))
              (iterate #(when (class? %) (.getSuperclass ^Class %)) cls)))


(defn- find-interfaces
  "Resolves all of the interfaces implemented by a class, both direct (through
  class ancestors) and indirect (through other interfaces)."
  [cls]
  (let [get-interfaces (fn [^Class c] (.getInterfaces c))
        direct-interfaces (mapcat get-interfaces (lineage cls))]
    (loop [queue (vec direct-interfaces)
           interfaces #{}]
      (if (empty? queue)
        interfaces
        (let [^Class iface (first queue)
              implemented (get-interfaces iface)]
          (recur (into (rest queue)
                       (remove interfaces implemented))
                 (conj interfaces iface)))))))


(defn inheritance-lookup
  "Builds a dispatcher which looks up a type by looking up the type itself,
  then attempting to look up its ancestor classes, implemented interfaces, and
  finally `java.lang.Object`."
  [dispatch]
  (fn lookup
    [t]
    (or
      ; Look up base class and ancestors up to the base class.
      (some dispatch (lineage t))

      ; Look up interfaces and collect candidates.
      (let [candidates (remove (comp nil? first)
                               (map (juxt dispatch identity)
                                    (find-interfaces t)))]
        (case (count candidates)
          0 nil
          1 (ffirst candidates)
          (throw (RuntimeException.
                   (format "%d candidates found for interfaces on dispatch type %s: %s"
                           (count candidates) t (str/join ", " (map second candidates)))))))

      ; Look up Object base class.
      (dispatch Object))))
