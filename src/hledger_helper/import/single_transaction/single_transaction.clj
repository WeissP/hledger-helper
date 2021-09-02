(ns hledger-helper.import.single-transaction.single-transaction
  (:require [hledger-helper.import.transactions :as tr]
            [cljc.java-time.local-date :as ld]
            [hledger-helper.processing-edn :as pedn]
            [hledger-helper.operations.currency :as cur]))


(defn- add-tr-help
  [k amt-dec date-bias]
  (let [info ((keyword k) (pedn/get-single-transactions))
        date (->> date-bias
                  (ld/minus-days (ld/now))
                  (ld/to-string))
        amount (if amt-dec
                 (-> amt-dec
                     cur/amt-dec->list
                     cur/list->amt-list
                     (cur/amt-list->amt "EURO"))
                 (:amount info))
        desc (or (:desc info) (str k))]
    (tr/write-transaction
      (tr/simple-transaction date
                             desc
                             [(tr/gen-posting (:act1 info) amount "euro")
                              (tr/gen-posting (:act2 info) "" "")]))))

(defn add-tr [k & [amt-dec]] (add-tr-help k amt-dec 0))
(defn add-tr- [k & [amt-dec]] (add-tr-help k amt-dec 1))
(defn add-tr-- [k & [amt-dec]] (add-tr-help k amt-dec 2))




