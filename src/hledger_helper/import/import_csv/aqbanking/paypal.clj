(ns hledger-helper.import.import-csv.aqbanking.paypal
  (:require [clojure.string :as string]
            [hledger-helper.import.import-csv.aqbanking.aqbanking :as aq]
            [clojure.java.shell :refer [sh]]
            [clojure.java.io :as io]))

(def paypal-dir (.getPath (io/resource "aqbanking/paypal/")))

(defn update-csv
  []
  (aq/delete-loc-files)
  (aq/gen-env "paypal" {})
  (sh (str paypal-dir "script.exp"))
  (println "finished"))

(def paypal-acount "asserts:paypal")

(defn to-transaction [paypal] (aq/to-transaction paypal paypal-acount))

(def convert-info
  {:type "paypal",
   :watch-dir paypal-dir,
   :sep \;,
   :date :date,
   :compare-fields
     {:remoteName (fn [res elem]
                    (let [check (fn [a b]
                                  (and (clojure.string/starts-with? a "From ")
                                       (clojure.string/starts-with? b "To ")))]
                      (case (count res)
                        1 (check (first res) (first elem))
                        2 (check (first res) (second res))
                        false))),
      :date nil},
   :remove-maps [{:remoteName
                    #(string/includes? % "Bank Account (direct debit)")}],
   :to-transaction to-transaction,
   :update-fn update-csv})


