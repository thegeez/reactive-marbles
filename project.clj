(defproject reactive-marbles "0.0.1"
  :description "Testing Netflix' RxJava with executable marble diagrams"
  :url "http://thegeez.net"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [com.netflix.rxjava/rxjava-clojure "0.6.1"]
                 [org.clojure/tools.logging "0.2.6"]]
  :profiles {:dev {:dependencies [[org.slf4j/slf4j-nop "1.7.2"]]}})
