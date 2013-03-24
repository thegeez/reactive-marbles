(ns reactive-marbles.core
  (:import rx.Observable
           rx.Observer
           rx.subjects.Subject
           rx.Notification
           rx.Notification$Kind
           java.util.concurrent.LinkedBlockingQueue)
  (:require [clojure.walk :as walk]
            [clojure.set :as set]
            [clojure.test :as test]
            [clojure.string :as string]
            [clojure.tools.logging :as log]))

(defn print-observer [name]
  (reify Observer
         (onNext [this value]
                 (log/info name "onNext" value))
         (onError [this error]
                  (log/info name "onError" error))
         (onCompleted [this]
                      (log/info name "onCompleted"))))

(defn eval-rx [& exprs]
  (eval `(do
           (import rx.Observable rx.Notification)
           ~@exprs)))


(defn hook-up-streams
  "hat tip to contextual-eval from joy of clojure"
  [ins expr]
  (let [ins-ks (zipmap (map first ins)
                       (map (comp symbol name first) ins))
        expr (clojure.walk/postwalk-replace ins-ks expr)]
     (eval-rx
     `(let [~@(let [[ss streams] (reduce (fn [[ss streams] [id s]]
                                           [(conj ss s `(Subject/create))
                                            (assoc streams id s)])
                                         [[] {}]
                                         ins-ks) ]
                (into ss ['all streams]))]
        [~'all ~expr]))))

(defn marble* [spec]
  (let [;; in case of no input streams
        spec (cond->> spec
                      (= (count spec) 4)
                      (into [:dummy-in [:-]]))

        [ins [[_ expr] [_ expected]]] (->> spec
                                           (partition 2)
                                           (partition 3 1)
                                           ((juxt (partial map first)
                                                  (comp rest last))))
        
        ;; pad ins to be as long as out
        expected-count (count expected)
        
        ins (for [[id ticks] ins]
              [id (take expected-count (concat ticks (repeat :-)))])
        
        expected (eval-rx expected)
                
        [streams out] (hook-up-streams ins expr)

        out-buf (LinkedBlockingQueue.)]
    (.subscribe (.materialize out)
                #(.put out-buf %))
    (.subscribe out (print-observer :out))
    (doseq [[id s] streams]
      (.subscribe s (print-observer id)))

    (let [ticks (apply map list (map (fn [[id ticks]]
                                       (map list (repeat id) ticks))
                                     ins))
          actual (reduce
                  (fn [out tick]
                    (doseq [[id tick] tick
                            :let [stream (streams id)]]
                      (condp = tick
                          :| (.onCompleted stream)
                          :X (.onError stream (ex-info "MarbleException" {:id id}))
                          :- nil  ;; noop
                          (.onNext stream tick)))
                    (conj out (if (.peek out-buf)
                                (let [notification (.take out-buf)]
                                  (log/info "Notification from buf: " notification)
                                  (condp = (.getKind notification)
                                      Notification$Kind/OnNext (.getValue notification)
                                      Notification$Kind/OnError :X
                                      Notification$Kind/OnCompleted :|))
                                :-)))
                  []
                  ticks)]
      {:expected expected
       :actual actual})))

(defmacro marble [spec]
  (let [decode {'- :-, 'X :X, '| :|}
        encode (set/map-invert decode)
        spec (mapcat (fn [[id ticks]]
                       [id (if (= id :expr)
                             ticks
                             (walk/postwalk-replace decode ticks))])
                     (partition 2 spec))]
    `(let [res# (marble* '~spec)]
       (zipmap (keys res#)
               (walk/postwalk-replace '~encode (vals res#))))))


(defmethod test/assert-expr 'marble [msg form]
           (let [newline (with-out-str (newline))
                 spec-str (str "["
                               (apply str (->> (second form)
                                               (partition 2)
                                               (map (partial string/join " "))
                                               (interpose (str newline " "))))
                               "]")]
             `(let [spec# (second '~form)
                    {expected# :expected
                     actual# :actual} ~form]
                (test/do-report {:type (if (= expected# actual#) :pass :fail),
                                 :message (str "Marble diagram test" ~newline ~spec-str),
                                 :expected expected#, :actual actual#})
                actual#)))
