(ns hledger-helper.import.import-csv.to-transaction
  (:require [clojure.string :as string]
            [hledger-helper.processing-edn :as pedn]
            [hledger-helper.import.transactions :as tr]))

(def date-reg #"\d{4}\-\d{2}\-\d{2}")

(defn- info->account
  "parse `info` to `account` by `import-transactions` and `special-info-account-maps`"
  [info & [special-info-account-maps]]
  (let [info (string/lower-case info)
        parse
          (fn [m]
            (some
              (fn [[k v]]
                (cond (string? k)
                        (when (string/includes? info (string/lower-case k)) v)
                      (vector? k) (when (every? #(string/includes?
                                                   info
                                                   (string/lower-case %))
                                                k)
                                    v)
                      :else nil))
              m))
        info-in-special (parse special-info-account-maps)]
    (if info-in-special info-in-special (parse @pedn/import-transactions))))

(defn to-transaction
  [info bank-date amount1 currency1 account2 &
   {:keys [amount2 currency2 special-info-account-maps desc-date?],
    :or {amount2 "", currency2 "", desc-date? false}}]
  (let [account1 (or (info->account info special-info-account-maps) "unknown")
        desc-date (when desc-date? (re-find date-reg info))
        real-date (or desc-date bank-date)
        desc (last (string/split account1 #":"))]
    (tr/simple-transaction
      real-date
      desc
      [(tr/gen-posting account1 amount1 currency1)
       (tr/gen-posting account2
                       amount2
                       currency2
                       (str (when desc-date (str "date:" bank-date))
                            (when (= account1 "unknown") info)))])))

(defn gen-info
  "concat values from all `keys` in `m`"
  [m keys]
  (->> keys
       (map #(first (% m)))
       (string/join " ")))

(defn same-assert-map?
  "`a` and `b` are the map need to be compared,
  `compare-fields` {:key fn}  `fn` receive the value from a and b with `key`, and return boolean
  if `fn` is nil, then use `eq` to compare
  return false if `compare-fields` is empty"
  [a b compare-fields]
  (not (or (empty? compare-fields)
           (nil? a)
           (nil? b)
           (empty? a)
           (empty? b)
           (some (fn [[k v]]
                   (let [f (or v #(= (last %1) (first %2)))]
                     (not (f (k a) (k b)))))
                 compare-fields))))

(defn merge-assert-maps
  "merge whole map list `l` with `same-assert-map?`"
  [compare-fields l]
  (reduce (fn [res elem]
            (let [last-elem (last res)
                  rest-elem (into [] (drop-last res))]
              (if (same-assert-map? last-elem elem compare-fields)
                (into rest-elem
                      ;; (println res elem)
                      ;; elem
                      [(merge-with (fn [& values] (apply concat values))
                                   last-elem
                                   elem)])
                (into res [elem]))))
    []
    l))

