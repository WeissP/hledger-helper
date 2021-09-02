(ns hledger-helper.import.to-file
  (:require [clojure.core.async :refer [chan go <! >!!]]
            [cljc.java-time.local-date-time :as ldt]))

(def spit-chan (chan 99))

(defn init
  "ready to write to file"
  []
  (go (loop []
        (spit (System/getenv "LEDGER_FILE") (<! spit-chan) :append true)
        (recur))))
;; (go (loop []
;;         (spit "test.journal" (<! spit-chan) :append true)
;;         (recur)))

(defn write [content] (>!! spit-chan (str "\n\n" content)))

(defn log
  [c]
  (spit "log.txt" (str c " " (ldt/to-string (ldt/now)) "\n") :append true))

