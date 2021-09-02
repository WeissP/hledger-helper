(defproject hledger-helper "0.1.0-SNAPSHOT"
  :description "a hledger helper"
  :url "http://example.com/"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0",
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [cli4clj "1.7.9"]
                 [org.clojure/data.csv "1.0.0"]
                 [org.clojure/core.match "1.0.0"]
                 [cljc.java-time "0.1.16"]
                 [doric "0.9.0"]
                 [org.clojure/core.async "1.3.618"]]
  :main hledger-helper.core
  :repl-options {:init-ns hledger-helper.core})
