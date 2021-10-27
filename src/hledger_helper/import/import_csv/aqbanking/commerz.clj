(ns hledger-helper.import.import-csv.aqbanking.commerz
  (:require [hledger-helper.import.import-csv.aqbanking.aqbanking :as aq]
            [clojure.java.io :as io]
            [clojure.java.shell :refer [sh]]))

(def commerz-dir (.getPath (io/resource "aqbanking/commerz/")))

(def commerz-acount "asserts:bank:commerz")

(defn update-csv
  []
  (aq/delete-loc-files)
  (aq/gen-env "commerz" {})
  (sh (str commerz-dir "cmds.sh")))

(defn to-transaction
  [commerz]
  (aq/to-transaction commerz commerz-acount {"paypal" "asserts:paypal"}))

(def convert-info
  {:type "commerz",
   :watch-dir commerz-dir,
   :sep \;,
   :date :date,
   :compare-fields {},
   :remove-maps [{:remoteAccountNumber empty?}],
   :to-transaction to-transaction,
   :update-fn update-csv})

