(ns hledger-helper.report.get-info
  (:require [clojure.java.shell :refer [sh]]
            [clojure.string :as string]
            [hledger-helper.operations.currency :as cur]
            [hledger-helper.operations.date :as date]))

(defn get-tr-by-assert-and-date
  "get all transactions by assert and date"
  [assert date]
  (-> (sh "hledger"
          "areg"
          (if (keyword? assert) (name assert) assert)
          (str "date:" date))
      :out
      (string/split-lines)
      rest
      ((fn [ls]
         (map (fn [l] (string/join " " (take 4 (string/split l #"\s+"))))
           ls)))))

(defn get-cost-within
  ([date-from date-to & categories]
   (->> (apply sh
          "hledger"
          "bal"
          "-1"
          "--cost"
          "-p"
          (str date-from ".." date-to)
          (if (empty? categories) ["expenses"] categories))
        :out
        (re-find #"\d+\,?\d+ €")
        cur/to-float)))

(defn get-cost-by-month
  [month & categories]
  (let [date-map (date/get-month-date month)]
    (apply get-cost-within
      (:date-from date-map)
      (:date-to date-map)
      categories)))

(defn get-costs-by-depth
  ([] (get-costs-by-depth (date/current-month) 2))
  ([depth] (get-costs-by-depth (date/current-month) depth))
  ([month depth]
   (let [date-map (date/get-month-date month)]
     (->> (sh "hledger"
              "bal"
              "expenses"
              (str "-" depth)
              "--cost"
              "-p"
              (str (:date-from date-map) ".." (:date-to date-map)))
          :out))))

