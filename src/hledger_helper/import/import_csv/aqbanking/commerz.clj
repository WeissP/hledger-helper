(ns hledger-helper.import.import-csv.aqbanking.commerz
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [clojure.java.shell :refer [sh]]
            [hledger-helper.import.import-csv.aqbanking.aqbanking :as aq]))

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
   :remove-maps [{:remoteBankCode #(= "00000000" (first %))}
                 {:remoteAccountNumber (comp empty? first),
                  :value_value #(string/starts-with? % "-")}],
   :to-transaction to-transaction,
   :update-fn update-csv})

