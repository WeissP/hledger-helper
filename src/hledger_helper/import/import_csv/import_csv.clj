(ns hledger-helper.import.import-csv.import-csv
  (:require [cljc.java-time.local-date :as ld]
            [clojure.java.io :as io]
            [hledger-helper.import.import-csv.csv :as csv]
            [hledger-helper.operations.operations :as op]
            [hledger-helper.import.import-csv.to-transaction :as tt]
            [hledger-helper.operations.date :as date]
            [hledger-helper.import.transactions :as tr]
            [clojure.string :as string]))

;; (csv/file->map-list
;;   "/home/weiss/clojure/hledger-helper/resources/aqbanking/paypal/paypal.csv"
;;   \;)

;; (csv/file->map-list
;;   "/home/weiss/clojure/hledger-helper/resources/aqbanking/commerz/commerz.csv"
;;   \;)

(defn scan-dir
  [dir sep]
  (->> dir
       io/file
       file-seq
       (filter #(.isFile %))
       (filter #(string/includes? (-> %
                                      .toPath
                                      .getFileName
                                      string/lower-case)
                                  ".csv"))
       (map #(csv/file->map-list % sep))
       flatten))

(defn import-single-assert
  [trs date-key remove-maps compare-fields to-transaction-fun]
  (some->> trs
           (op/remove-map remove-maps)
           not-empty
           (#(date/sort-maps-by-date % :date-key date-key))
           (tt/merge-assert-maps compare-fields)
           (map to-transaction-fun)
           (tr/write-transactions)))

;; (defn import-single-assert
;;   "import by given maps `m`"
;;   [m]
;;   (let [watch-dir (:watch-dir m)
;;         date-key (:date m)
;;         remove-maps (:remove-maps m)
;;         sep (:sep m)
;;         compare-fields (:compare-fields m)
;;         to-transaction (:to-transaction m)
;;         sort-maps (fn [maps]
;;                     (sort-by #(-> %
;;                                   date-key
;;                                   first
;;                                   date/convert-date
;;                                   ld/parse)
;;                              ld/compare-to
;;                              maps))]
;;     (some->> watch-dir
;;              io/file
;;              file-seq
;;              (filter #(.isFile %))
;;              (filter #(string/includes? (-> %
;;                                             .toPath
;;                                             .getFileName
;;                                             string/lower-case)
;;                                         ".csv"))
;;              (map #(csv/file->map-list % sep))
;;              flatten
;;              (op/remove-map remove-maps)
;;              not-empty
;;              sort-maps
;;              (tt/merge-assert-maps compare-fields)
;;              (map to-transaction)
;;              (tr/write-transactions))))

