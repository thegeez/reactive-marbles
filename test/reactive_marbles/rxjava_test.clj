(ns reactive-marbles.rxjava-test
  (:use clojure.test
        reactive-marbles.core))

(deftest marble-test-empty
    (is (marble [:src []
                 :expr :src 
                 :out []])))

(deftest marble-test-error
    (is (marble [:src [X]
                 :expr :src 
                 :out [X]])))

(deftest marble-test-completed
    (is (marble [:src [|]
                 :expr :src 
                 :out [|]])))

(deftest flip-example
  (is (marble [:src [ 1  2  3 4 5 6 | ]
               :expr (.map :src (fn [value]
                                  (if (= value 4)
                                       (throw (Exception. "DemoFail on 4"))
                                       (* -1 value))))
               :out [-1 -2 -3 X]])))

(deftest empty-test
    (is (marble [:expr (Observable/empty)
                 :out [|]])))

(deftest just-test
    (is (marble [:expr (Observable/just 1)
                 :out [ 1 |]])))

(deftest filter-test
    (is (marble [:src [1 2 3 4 5 6 |]
                 :expr (.filter :src (comp boolean #{1 4}))
                 :out [1 - - 4 - - |]])))

(deftest filter-empty
      (is (marble [:src [|]
                   :expr (.filter :src (comp boolean #{1 4}))
                   :out [|]])))

(deftest map-test
  (is (marble [:src [ 1 2 3 4 5 6 |]
               :expr (.map :src inc)
               :out [ 2 3 4 5 6 7 |]])))

(deftest map-many-test
  (is (marble [:src [ 1 - 2 - 3 |]
               :expr (.mapMany :src (fn [v]
                                      (Observable/from [v v])))
               :out [ 1 1 2 2 3 3 |]])))

(deftest materialize-test
  (is (marble [:src [ 1 2 |]
               :expr (.materialize :src)
               :out [ (Notification. 1) (Notification. 2) (Notification.) |]])))

(deftest merge-test
  (is (marble [:in1 [1 - 3 - X]
               :in2 [- 2 - 4 - 6 |]
               :expr (Observable/merge [:in1 :in2])
               :out [1 2 3 4 X]])))

(deftest concat-test
  (is (marble [:in1 [1 2 3 |]
               :in2 [- - 4 5 6 |]
               :expr (Observable/concat (into-array Observable [:in1 :in2]))
               :out [1 2 3 5 6 |]])))

(deftest concat-overlap
  (is (marble [:in1 [1 2 3 |]
               :in2 [- - 4 5 6 |]
               :expr (Observable/concat (into-array Observable [:in1 :in2]))
               :out [1 2 3 5 6 |]])))

(deftest merge-delay-error
  (is (marble [:in1 [1 - 3 - X]
               :in2 [- 2 - 4 - 6 |]
               :expr (Observable/mergeDelayError [:in1 :in2])
               :out [1 2 3 4 - 6 X]])))

(deftest on-error-resume-next
  (is (marble [:in1 [1 2 3 X]
               :in2 [- - - 4 5 |]
               :expr (Observable/onErrorResumeNext :in1 :in2)
               :out [1 2 3 4 5 |]])))

(deftest on-error-resume-next-overlap
  (is (marble [:in1 [1 2 3 X]
               :in2 [- - 4 5 6 |]
               :expr (Observable/onErrorResumeNext :in1 :in2)
               :out [1 2 3 5 6 |]])))

(deftest on-error-resume-next-fn
  (is (marble [:src [1 2 3 X]
               :expr (Observable/onErrorResumeNext :src (fn [error]
                                                          (Observable/from [5 6])))
               :out [1 2 3 5 6 |]])))

(deftest on-error-resume-next-no-error
  (is (marble [:in1 [1 2 3 |]
               :in2 [- - 4 5 6 |]
               :expr (Observable/onErrorResumeNext :in1 :in2)
               :out [1 2 3 |]])))

(deftest reduce-test
  (is (marble [:src [1 2 3 |]
               :expr (Observable/reduce :src +)
               :out [- - - 6 |]])))

(deftest reduce-test-seed
  (is (marble [:src  [1 2 3 |]
               :expr (Observable/reduce :src 3 +)
               :out [- - - 9 |]])))

(deftest scan-test
  (is (marble [:src [1 2 3 |]
               :expr (Observable/scan :src +)
               :out [- 1 3 6 |]])))

;; note the diagram in javadoc is wrong with regard to the seed
(deftest scan-test-seed
  (is (marble [:src [1 2 3 |]
               :expr (Observable/scan :src 3 +)
               :out [3 4 6 9 |]])))

(deftest skip-test
  (is (marble [:src [1 2 3 4 5 |]
               :expr (Observable/skip :src 3)
               :out [- - - 4 5 |]])))

;; test fails and demos bug in rxjava 0.6.1
;; see https://github.com/Netflix/RxJava/pull/197
(deftest take-test
  (is (marble [:src [1 2 3 4 5 |]
               :expr (Observable/take :src 2)
               :out [1 2 |]])))

;; root cause for take fail
(deftest take-while-test
  (is (marble [:src [1 2 3 4 5 |]
               :expr (Observable/takeWhile :src (fn [i] (< i 3)))
               :out [1 2 |]])))

;; toList fails with multiple subscribers and demos bug in rxjava 0.6.1
;; the multiple observers are an implementation detail of marble
;; see https://github.com/Netflix/RxJava/pull/206
(deftest list-test
  (is (marble [:src [1 2 3 4 5 6 |]
               :expr (Observable/toList :src)
               :out [- - - - - - [1 2 3 4 5 6] | ]])))

(deftest to-observable-test
  (is (marble [:expr (Observable/toObservable [1 2 3 4 5 6])
               :out [1 2 3 4 5 6 |]])))

;; fails due to toList fail
(deftest to-sorted-list
  (is (marble [:src [2 5 1 6 3 4 |]
               :expr (Observable/toSortedList :src)
               :out [- - - - - - [1 2 3 4 5 6] | ]])))

(deftest zip-test
  (is (marble [:in1 [1 - 3 - 5 |]
               :in2 [- 2 - 4 - 6 |]
               :expr (Observable/zip :in1 :in2 +)
               :out [- 3 - 7 - 11 |]])))

(deftest zip-list
  (is (marble [:in1 [1 -     3 -     5 |]
               :in2 [- 2     - 4     - 6 |]
               :expr (Observable/zip :in1 :in2 list)
               :out [- [1 2] - [3 4] - [5 6] |]])))

(deftest zip-three
  (is (marble [:in1 [1 - - 4 - -  7 |]
               :in2 [- 2 - - 5 -  - 8 |]
               :in3 [- - 3 - - 6  - - 9 |]
               :expr (Observable/zip :in1 :in2 :in3 +)
               :out [- - 6 - - 15 - - 24 |]])))

(deftest zip-four
  (is (marble [:in1 [1 - -  5 -  -  9 |]
               :in2 [- 2 -  - 6  -  10 |]
               :in3 [- - 3  7 -  11 |]
               :in4 [- 4 -  - 8  -  12 | ]
               :expr (Observable/zip :in1 :in2 :in3 :in4 +)
               :out [- - 10 - 26 - 42 |]])))


