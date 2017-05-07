(ns thekeep.async
  (:require [cljs.core.async.macros :as m]
            [cljs.core.async.macros :refer [go]]))

;; macros for more convenient game async
(defmacro <!* [test & body]
  `(let [result# (cljs.core.async/<! ~@body)]
     (if ~test
       result#
       (throw (js/Error "go-while exit")))))

(defmacro >!* [test & body]
  `(let [result# (cljs.core.async/>! ~@body)]
     (if ~test
       result#
       (throw (js/Error "go-while exit")))))

(defn thread-test-around-sync-points [test body]
  (let [[head & tail] body]
    (cond
      ;; thread test condition around <!
      (= head '<!)
      (thread-test-around-sync-points test
               (cons 'thekeep.async/<!* (cons test tail)))

      (= head '>!)
      (thread-test-around-sync-points test
               (cons 'thekeep.async/>!* (cons test tail)))

      :default
      (for [a body]
        (cond
          (symbol? a) a

          ;; recurse through subforms of a vector
          (vector? a) (into [] (thread-test-around-sync-points test a))

          ;; recurse through this full s-exp form
          (sequential? a) (thread-test-around-sync-points test a)

          ;; default is leave code unchanged
          :default a)))))

(defmacro foo-while [test & body]
  (thread-test-around-sync-points test (cons 'do body)))

(defmacro go-while [test & body]
  `(go
     ~(thread-test-around-sync-points test (cons 'do body))))

(defmacro continue-while [test & body]
  `(try
     ~(thread-test-around-sync-points test (cons 'do body))
     (catch js/Error e
       (js/console.log "continue-while exit triggered by: " e))))
