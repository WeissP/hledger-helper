(ns hledger-helper.import.transactions
  (:require [clojure.string :as string]
            [hledger-helper.operations.currency :as cur]
            [hledger-helper.import.to-file :as tf]))


(defrecord TransactionRecord [date status code description posting])

(defn gen-posting
  "generate posting lines"
  [account amount currency-name & comments]
  (let [c (if (> (count (first comments)) 0)
            (str "  ; " (string/join " " comments))
            "")]
    (str account
         "   "
         (when-not (= currency-name "") (cur/display amount currency-name))
         c)))

(defn simple-transaction
  "create simple TransactionRecord"
  [date description posting]
  (TransactionRecord. date "" "" description posting))


(defn to-string
  [transaction]
  (if transaction
    (let [[date status code description posting]
            (map #(% transaction) [:date :status :code :description :posting])]
      (str date
           " " status
           " " code
           " " description
           "\n" (string/join "\n" (map #(str "    " %) posting))))
    ""))

(defn write-transaction [transaction] (tf/write (to-string transaction)))

(defn write-transactions
  [transactions]
  (tf/write (string/join "\n\n" (map to-string transactions))))


