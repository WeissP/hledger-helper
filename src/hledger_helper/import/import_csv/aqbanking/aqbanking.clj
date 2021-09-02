(ns hledger-helper.import.import-csv.aqbanking.aqbanking
  (:require [clojure.java.io :as io]
            [cljc.java-time.local-date :as ld]
            [cljc.java-time.format.date-time-formatter :as fmt]
            [clojure.string :as string]
            [hledger-helper.processing-edn :as pedn]
            [hledger-helper.operations.date :as date]
            [hledger-helper.import.import-csv.to-transaction :as tt]
            [hledger-helper.operations.currency :as cur]
            [clojure.java.shell :refer [sh]]))

(defn delete-loc-files
  "delete lock files to force aqbanking run without asking "
  ([] (delete-loc-files "/home/weiss/.aqbanking/settings6"))
  ([dir]
   (->> dir
        io/file
        file-seq
        (filter #(.isFile %))
        (filter #(string/includes? (.getFileName (.toPath %)) ".lck"))
        (map #(io/delete-file (.getAbsolutePath %))))))

(defn get-path-by-assert
  [assert]
  (str (.getPath (io/resource (str "aqbanking/" assert))) "/"))

(defn date->str
  "convert `date` to the aqbanking date format"
  [date]
  (ld/format (date/to-local-date date) (fmt/of-pattern "yyyyMMdd")))

(defn gen-date-range
  "generate date range from last-update-date to yesterday"
  [assert]
  {:fromdate (date->str (pedn/get-last-update-date (keyword assert))),
   :todate (date->str (ld/now))})

(defn gen-env
  "generate env.sh"
  [assert env]
  (let [dir (get-path-by-assert assert)
        default-env (merge (gen-date-range assert) {:temp (str dir "temp")})]
    (spit (str dir "env.sh")
          (string/join "\n"
                       (map (fn [[k v]] (str (name k) "=" v))
                         (merge default-env env))))))

(defn gen-csv
  [in & [out-file]]
  (let [args ["aqbanking-cli" "export" "--exporter=csv" "-c" in]]
    (:out (apply sh (if out-file (conj args out-file) args)))))


(defn to-transaction
  "general transactions generator for all aqbanking accounts"
  [m account2 & [special-info-account-maps]]
  (let [info (tt/gen-info m [:remoteName :purpose :purpose1])
        bank-date (date/convert-date (first (:date m)))
        currencies (:value_currency m)
        amounts (:value_value m)
        [amount2 amount1]
          (if (= (count amounts) 3)
            (map #(cur/convert-amount %1
                                      :number-notation-from "USD"
                                      :number-notation-to %2
                                      :operation "keep")
              amounts
              currencies)
            [""
             (cur/convert-amount (first amounts)
                                 :number-notation-from "USD"
                                 :operation "reverse")])
        [currency2 currency1]
          (if (= (count currencies) 3) currencies ["" (first currencies)])]
    (tt/to-transaction info
                       bank-date
                       amount1
                       currency1
                       account2
                       :amount2 amount2
                       :currency2 currency2
                       :special-info-account-maps special-info-account-maps)))

