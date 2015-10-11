(ns puget.dispatch
  "This namespace contains functions for building _dispatch functions_ over
  value types. This affords similar functionality as Clojure's protocols, but
  operates over locally-constructed data structures rather than modifying a
  global dispatch table.

  A dispatch function takes a single argument of type `Class` and should return
  the looked-up value. A simple example is a map from classes to values, which
  can be used directly as a lookup function."
  (:require
    [clojure.string :as str]))


(defn fallback-lookup
  "Builds a dispatcher which looks up a type by checking multiple dispatchers
  in order until a matching entry is found."
  ([a] a)
  ([a b & more]
   (let [candidates (list* a b more)]
     (fn lookup [t]
       (some #(% t) candidates)))))


(defn caching-lookup
  "Builds a dispatcher which caches values returned for each type. This improves
  performance when the underlying dispatcher may need to perform complex
  lookup logic to determine the dispatched value."
  [dispatch]
  (let [cache (atom {})]
    (fn lookup [t]
      (or (get @cache t)
          (when-let [v (dispatch t)]
            (swap! cache assoc t v)
            v)))))


(defn inheritance-lookup
  "Builds a dispatcher which looks up a type by looking up the type itself,
  then attempting to look up its ancestor classes, implemented interfaces, and
  finally `java.lang.Object`."
  [dispatch]
  (fn lookup [t]
    (let [superclasses (take-while (partial not= Object)
                                   (iterate #(.getSuperclass ^Class %) t))]
      (or
        ; Look up base type.
        (dispatch t)

        ; Look up base classes up to Object.
        (some dispatch superclasses)

        ; Look up interfaces and collect candidates.
        (let [interfaces (mapcat #(.getInterfaces ^Class %) superclasses)
              candidates (remove (comp nil? second)
                                 (map (juxt identity dispatch)
                                      interfaces))]
          (case (count candidates)
            0 nil
            1 (second (first candidates))
            (throw (RuntimeException.
                     (format "%d candidates found for interfaces on dispatch type %s: %s"
                             (count candidates) t (str/join ", " (map first candidates)))))))

        ; Look up Object base class.
        (dispatch Object)))))
