(ns hledger-helper.processing-edn
  (:require [clojure.edn :as edn]
            [hledger-helper.operations.date :as date]
            [clojure.pprint :as pprint]
            [clojure.string :as string]))

(def filepath "/home/weiss/finance/info.edn")

(defn read-edn [] (edn/read-string (slurp filepath)))

(defn pretty-spit
  [file-name collection]
  (spit (java.io.File. file-name)
        (with-out-str
          (pprint/write collection :dispatch pprint/code-dispatch))))

(defn write-edn [m] (pretty-spit filepath m))

(defn add-assert
  [assert]
  (doseq [k [:last-transaction-date :start-date]]
    (write-edn
      (update-in (read-edn) [:asserts (keyword assert) k] (constantly nil)))))

(defn update-last-update-date
  [assert date]
  (write-edn (update-in (read-edn)
                        [:asserts (keyword assert) :last-update-date]
                        (constantly date))))

(defn update-rollover
  "update rollover-amount to `amount` and increase rollover-month"
  [amount]
  (write-edn (-> (read-edn)
                 (update-in [:budget :rollover :month] date/inc-month)
                 (update-in [:budget :rollover :amount] (constantly amount)))))

(defn update-budget-mod-amount
  "update non-zero budget mod amount, if updated amount is over or equal zero, then amount will be set to 0 and this modifier will be moved to finished-modifiers and return the orginal amount, otherwise return `nil`"
  [k delta-amount]
  (let [v (get-in (read-edn) [:budget :modifiers k])
        ori-amount (:amount v)]
    (cond
      (= ori-amount 0)
        (throw (Exception. (str
                             "amount is already zero, can not be modified!")))
      (<= (* ori-amount (- ori-amount delta-amount)) 0)
        (do (write-edn (-> (read-edn)
                           (update-in [:budget :modifiers] dissoc k)
                           (update-in [:budget :finished-modifiers k]
                                      (constantly (assoc v :amount 0)))))
            (if (= ori-amount delta-amount) nil ori-amount))
      :else (write-edn (update-in (read-edn)
                                  [:budget :modifiers k :amount]
                                  #(- % delta-amount))))))

(defn budget-transfer
  "transfer rollover-amount to the amount from modifier with key `key-str`"
  [key-str delta-amount]
  (let [k (keyword key-str)
        transfered-amount (or (update-budget-mod-amount k delta-amount)
                              delta-amount)]
    (write-edn (update-in (read-edn)
                          [:budget :rollover :amount]
                          #(+ % transfered-amount)))))

(defn get-last-update-date
  [assert]
  (get-in (read-edn) [:asserts (keyword assert) :last-update-date]))

(defn get-all-asserts [] (keys (:asserts (read-edn))))

(defn get-budget-amount [] (get-in (read-edn) [:budget :amount]))
(defn get-budget-rollover [] (get-in (read-edn) [:budget :rollover]))
(defn get-budget-mod [] (get-in (read-edn) [:budget :modifiers]))
(defn get-budget-exclusive-expenses
  []
  (get-in (read-edn) [:budget :exclusive-expenses]))

(defn get-single-transactions
  "flatten single-transactions to one layer maps, and return it"
  []
  (apply merge
    (map
      (fn [[k v]]
        (let [children (:children v)
              children-keys (map (fn [e] (if (map? e) (first (keys e)) e))
                              children)
              main-info (dissoc v :children)
              children-spec-info (filter map? children)]
          (apply merge-with
            merge
            (zipmap (conj children-keys k) (repeat main-info))
            children-spec-info)))
      (get-in (read-edn) [:single-transactions]))))

(def ^:private get-import-transactions-by-maps
  "processing import-transactions (`map`) to {'string must include', 'account'}"
  (memoize
    (fn [maps]
      (let [flatten-keys
              (fn [m]
                (apply merge
                  (map (fn [[k v]]
                         (cond (nil? v) {k (last (string/split (name k) #":"))}
                               (map? v) (apply merge
                                          (map (fn [[kk vv]]
                                                 {(str (name k) ":" (name kk))
                                                    vv})
                                            v))
                               :else {k v}))
                    m)))
            flatted-maps (loop [m maps]
                           (let [mm (flatten-keys m)]
                             (if (= m mm) m (recur mm))))]
        (zipmap (vals flatted-maps) (keys flatted-maps))))))

(defn get-import-transactions
  "get import-transactions stored in edn file, result will be memoized by same import-transactions"
  []
  (get-import-transactions-by-maps (:import-transactions (read-edn))))

(def import-transactions (atom (get-import-transactions)))

(defn update-import-transactions
  []
  (reset! import-transactions (get-import-transactions)))

