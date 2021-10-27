(ns hledger-helper.import.import-csv.aqbanking.auto-import
  (:require [hledger-helper.import.import-csv.aqbanking.commerz :as commerz]
            [hledger-helper.import.import-csv.aqbanking.paypal :as paypal]
            [hledger-helper.import.import-csv.aqbanking.aqbanking :as aq]
            [hledger-helper.import.import-csv.import-csv :as ic]
            [hledger-helper.operations.date :as date]
            [cljc.java-time.local-date :as ld]
            [hledger-helper.import.to-file :as tf]
            [hledger-helper.processing-edn :as pedn]
            [hledger-helper.import.import-csv.csv :as csv]))

(def asserts-map {:commerz commerz/convert-info, :paypal paypal/convert-info})



(defn merge-date-filter
  "add date filter with aqbanking/gen-date to `remove-maps`"
  [convert-info]
  (merge-with
    merge
    convert-info
    {:remove-maps {:date #(let [date-range (aq/gen-date-range
                                             (keyword (:type convert-info)))
                                d (first %)]
                            (not (and (date/eq-later? d (:fromdate date-range))
                                      (date/eq-later? (:todate date-range)
                                                      d))))}}))

(defn updatable?
  "check if current assert is updatable"
  [assert]
  (date/later? (ld/now) (pedn/get-last-update-date (keyword assert))))

(defn get-new-trs
  [m]
  (let [dir (:watch-dir m)
        data-path (str dir "temp")
        csv-path (str dir "output.csv")
        data (do ((:update-fn m)) (aq/gen-csv data-path))
        old-map (csv/file->map-list csv-path \;)
        new-map (csv/csv->map-list (csv/str->csv data \;))
        new-tr (csv/get-diff old-map new-map)]
    (when-not (empty? new-tr)
      (csv/append-map-list csv-path new-tr)
      ;; (if (> (count old-map) 100)
      ;;   (spit csv-path data)
      ;;   (spit csv-path (str "\n" data) :append true))
    )
    new-tr))

(defn import-single-assert
  [assert-key assert-value]
  (let [m (merge-date-filter assert-value)
        trs (get-new-trs m)
        date-key :date
        remove-maps (:remove-maps m)
        compare-fields (:compare-fields m)
        to-transaction-fun (:to-transaction m)]
    (when (ic/import-single-assert trs
                                   date-key
                                   remove-maps
                                   compare-fields
                                   to-transaction-fun)
      (pedn/update-last-update-date assert-key (ld/to-string (ld/now))))))

(defn import-all
  []
  (pedn/update-import-transactions)
  (doseq [[k v] asserts-map] (import-single-assert k v)))

(defn init
  []
  (future (loop []
            (tf/log "update")
            (import-all)
            (Thread/sleep (* 1000 60 60 2))
            (recur))))

;; (import-all)
